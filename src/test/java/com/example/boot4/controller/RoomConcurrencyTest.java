package com.example.boot4.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 50 유저 동시접속 채팅방 생성/조회 - 실제 HTTP 요청 동시성 테스트
 *
 * ✅ java.net.http.HttpClient + RANDOM_PORT 방식 선택 이유
 * --------------------------------------------------
 * Spring Boot 4.x 에서 TestRestTemplate 이 제거되었으므로
 * JDK 내장 HttpClient (java.net.http.HttpClient) 를 사용한다.
 * HttpClient 는 Virtual Thread 와 자연스럽게 통합되며
 * 비동기/동기 모두 지원한다.
 *
 * 요청 흐름:
 * Virtual Thread → HttpClient (JDK) → TCP → 내장 Tomcat
 * → DispatcherServlet → RoomController → RoomService
 * → Hibernate → SQLite (WAL 모드)
 *
 * ⚠️ 정리 전략 (@AfterAll)
 * --------------------------------------------------
 * HTTP 요청은 서버 내부에서 트랜잭션이 커밋된 후 응답이 돌아온다.
 * 테스트 메서드에 @Transactional 을 걸어도 이미 DB 에 커밋된 이후이므로
 * 롤백 대상이 없다. 따라서 모든 테스트가 끝난 뒤 @AfterAll 에서
 * 원하는 roomId 들을 한 번에 종합 삭제한다.
 * 이렇게 하면 테스트 간 충돌 없이 실제 데이터를 누적해 테스트할 수 있다.
 *
 * SQLite 동시성 설정 (application.yml):
 * - journal_mode=WAL : 읽기/쓰기 동시 실행 가능
 * - busy_timeout=60000 : SQLITE_BUSY 시 60초 재시도
 * - HikariCP max-pool-size=100 : 50 동시 요청 모두 즉시 커넥션 획득
 *
 * 실행 방법:
 * JASYPT_KEY="..." ./gradlew test \
 * --tests "com.example.boot4.controller.RoomConcurrencyTest" --info
 */
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // non-static @AfterAll 허용
@TestMethodOrder(MethodOrderer.DisplayName.class) // DisplayName 알파벳순 (순차 실행)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RoomConcurrencyTest {

    private static final int CONCURRENT_USERS = 50;
    /**
     * pool=1 시 SQLite 쓰기 시간: ~100ms/req * 50 = ~5s.
     * 30초면 충분하며, 초과 시빠른 실패 확인 가능.
     */
    private static final long TIMEOUT_SECONDS = 30;

    /** JSON 에서 "id":숫자 를 추출하는 패턴 */
    private static final Pattern ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");

    @LocalServerPort
    private int port;

    /** JDBC 직접 실행용 - JPA/Hibernate 트랜잭션 락 없이 클린업/검증 */
    @Autowired
    private DataSource dataSource;

    /** JDK HttpClient - Virtual Thread executor 사용 */
    private final HttpClient httpClient = HttpClient.newBuilder()
            .executor(Executors.newVirtualThreadPerTaskExecutor())
            .build();

    /** 테스트 전 과정에서 생성된 roomId 를 전체 수집 → @AfterAll 에서 한 번에 정리 */
    private final List<Long> createdRoomIds = Collections.synchronizedList(new ArrayList<>());

    private String roomsUrl() {
        return "http://localhost:" + port + "/api/rooms";
    }

    /**
     * @BeforeAll: 테스트 전 단 1회. JDBC 네이티브 SQL로 완전 클린.
     *             JPA/Hibernate 트랜잭션 락 없이 단일 커넥션으로 DELETE.
     *             실패 시 예외 던져 테스트 자체를 설정 오류로 즐시 종료.
     */
    @BeforeAll
    void initDb() throws Exception {
        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM chats");
            stmt.execute("DELETE FROM rooms");
            log.info("✅ [BeforeAll] DB 초기화 완료 (rooms, chats 전체 삭제)");
        }
        // 예외 발생 시 자동으로 테스트 런너가 실패 처리
    }

    /**
     * @AfterAll: 모든 테스트 완료 후 단 1회.
     *            JDBC 네이티브 SQL로 일괄 삭제. 실패 시 경고만 (테스트 로직 무관하게 유지).
     */
    @AfterAll
    void cleanupAll() {
        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM chats");
            stmt.execute("DELETE FROM rooms");
            log.info("✅ [AfterAll] 정리 완료");
        } catch (Exception e) {
            log.warn("⚠️ [AfterAll] 정리 실패 - 수동 삭제 필요: {}", e.getMessage());
        }
    }

    /** JSON body 로 POST 요청을 보내는 헬퍼 */
    private HttpRequest buildPostRequest(String title) {
        String body = "{\"title\":\"" + title + "\"}";
        return HttpRequest.newBuilder()
                .uri(URI.create(roomsUrl()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
    }

    /** GET 요청을 보내는 헬퍼 */
    private HttpRequest buildGetRequest() {
        return HttpRequest.newBuilder()
                .uri(URI.create(roomsUrl()))
                .GET()
                .build();
    }

    /** 응답 body 에서 id 값 추출 */
    private Long extractId(String responseBody) {
        Matcher m = ID_PATTERN.matcher(responseBody);
        return m.find() ? Long.parseLong(m.group(1)) : null;
    }

    // -------------------------------------------------------------------------
    // 테스트 1: 채팅방 생성 - 500명 동시 POST /api/rooms
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/rooms - 50 유저 동시 채팅방 생성 (실제 HTTP)")
    void concurrentRoomCreation() throws Exception {
        log.info("=".repeat(60));
        log.info("🚀 테스트: {}명 동시 POST /api/rooms | 타임아웃 {}초", CONCURRENT_USERS, TIMEOUT_SECONDS);
        log.info("=".repeat(60));

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch ready = new CountDownLatch(CONCURRENT_USERS);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(CONCURRENT_USERS);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<String> errors = Collections.synchronizedList(new ArrayList<>());
        List<Long> currentTestIds = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < CONCURRENT_USERS; i++) {
            final int userId = i;
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();

                    HttpResponse<String> response = httpClient.send(
                            buildPostRequest("동시성_방_" + userId),
                            HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 201) {
                        Long id = extractId(response.body());
                        if (id != null) {
                            createdRoomIds.add(id);
                            currentTestIds.add(id);
                        }
                        successCount.incrementAndGet();
                        log.debug("✅ 유저 {} → POST 201 (id={})", userId, id);
                    } else {
                        failCount.incrementAndGet();
                        errors.add("유저 " + userId + ": HTTP " + response.statusCode() + " - " + response.body());
                        log.warn("❌ 유저 {} → HTTP {} | body: {}", userId, response.statusCode(), response.body());
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    failCount.incrementAndGet();
                    errors.add("유저 " + userId + ": interrupted");
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    errors.add("유저 " + userId + ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
                    log.warn("❌ 유저 {} 예외: {}", userId, e.getMessage());
                } finally {
                    done.countDown();
                }
            });
        }

        boolean allReady = ready.await(15, TimeUnit.SECONDS);
        assertThat(allReady).as("모든 Virtual Thread 가 15초 내 준비 완료되어야 함").isTrue();

        long startTime = System.currentTimeMillis();
        start.countDown(); // 🏁 일제히 시작!

        boolean allDone = done.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - startTime;
        executor.shutdown();

        printResult("POST /api/rooms", successCount.get(), failCount.get(), elapsed, errors);

        assertThat(allDone)
                .as("⚠️ %d초 초과! Virtual Thread 핀닝 또는 HikariCP 풀 고갈 의심", TIMEOUT_SECONDS)
                .isTrue();
        assertThat(failCount.get())
                .as("HTTP 실패 건수는 0이어야 함. 오류 목록 (샘플): %s", errors.stream().limit(5).toList())
                .isZero();
        assertThat(successCount.get()).isEqualTo(CONCURRENT_USERS);

        long savedCount = countByIdJdbc(currentTestIds);
        assertThat(savedCount)
                .as("DB 저장 수(%d)가 HTTP 성공 수(%d)와 일치해야 함", savedCount, successCount.get())
                .isEqualTo(successCount.get());

        log.info("✅ 동시 생성 테스트 통과! 소요: {}ms", elapsed);
    }

    // -------------------------------------------------------------------------
    // 테스트 2: 채팅방 목록 조회 - 500명 동시 GET /api/rooms
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/rooms - 50 유저 동시 채팅방 목록 조회 (실제 HTTP)")
    void concurrentRoomListing() throws Exception {
        log.info("=".repeat(60));
        log.info("🚀 테스트: 사전 방 생성 후 {}명 동시 GET /api/rooms", CONCURRENT_USERS);
        log.info("=".repeat(60));

        // 사전 데이터: 방 10개 HTTP POST 로 생성
        int preInsertCount = 10;
        for (int i = 0; i < preInsertCount; i++) {
            HttpResponse<String> resp = httpClient.send(
                    buildPostRequest("사전_방_" + i),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(resp.statusCode()).isEqualTo(201);
            Long id = extractId(resp.body());
            if (id != null)
                createdRoomIds.add(id);
        }
        log.info("✅ 사전 방 {}개 생성 완료", preInsertCount);

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch ready = new CountDownLatch(CONCURRENT_USERS);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(CONCURRENT_USERS);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<String> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < CONCURRENT_USERS; i++) {
            final int userId = i;
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();

                    HttpResponse<String> response = httpClient.send(
                            buildGetRequest(),
                            HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200) {
                        successCount.incrementAndGet();
                        log.debug("✅ 유저 {} → GET 200", userId);
                    } else {
                        failCount.incrementAndGet();
                        errors.add("유저 " + userId + ": HTTP " + response.statusCode() + " - " + response.body());
                        log.warn("❌ 유저 {} → HTTP {} | body: {}", userId, response.statusCode(), response.body());
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    failCount.incrementAndGet();
                    errors.add("유저 " + userId + ": interrupted");
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    errors.add("유저 " + userId + ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
                    log.warn("❌ 유저 {} 예외: {}", userId, e.getMessage());
                } finally {
                    done.countDown();
                }
            });
        }

        boolean allReady = ready.await(15, TimeUnit.SECONDS);
        assertThat(allReady).as("모든 Virtual Thread 가 15초 내 준비 완료되어야 함").isTrue();

        long startTime = System.currentTimeMillis();
        start.countDown(); // 🏁 일제히 시작!

        boolean allDone = done.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - startTime;
        executor.shutdown();

        printResult("GET /api/rooms", successCount.get(), failCount.get(), elapsed, errors);

        assertThat(allDone)
                .as("⚠️ %d초 초과! 핀닝 또는 풀 고갈 의심", TIMEOUT_SECONDS)
                .isTrue();
        assertThat(failCount.get())
                .as("HTTP 실패 건수는 0이어야 함. 오류 목록 (샘플): %s", errors.stream().limit(5).toList())
                .isZero();

        log.info("✅ 동시 조회 테스트 통과! 소요: {}ms", elapsed);
    }

    // -------------------------------------------------------------------------
    // 테스트 3: 읽기/쓰기 혼합 - 250명 POST + 250명 GET 동시 실행
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST + GET /api/rooms - 50 유저 읽기/쓰기 혼합 동시 요청 (실제 HTTP)")
    void concurrentMixedReadWrite() throws Exception {
        log.info("=".repeat(60));
        log.info("🚀 테스트: 읽기/쓰기 혼합 {}명 동시 HTTP 요청", CONCURRENT_USERS);
        log.info("=".repeat(60));

        int writers = CONCURRENT_USERS / 2;
        int readers = CONCURRENT_USERS / 2;
        int total = writers + readers;

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch ready = new CountDownLatch(total);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(total);

        AtomicInteger writeSuccess = new AtomicInteger(0);
        AtomicInteger readSuccess = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<String> errors = Collections.synchronizedList(new ArrayList<>());
        List<Long> currentTestIds = Collections.synchronizedList(new ArrayList<>());

        // Writer 스레드 (POST)
        for (int i = 0; i < writers; i++) {
            final int idx = i;
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    HttpResponse<String> resp = httpClient.send(
                            buildPostRequest("혼합_방_" + idx),
                            HttpResponse.BodyHandlers.ofString());
                    if (resp.statusCode() == 201) {
                        Long id = extractId(resp.body());
                        if (id != null) {
                            createdRoomIds.add(id);
                            currentTestIds.add(id);
                        }
                        writeSuccess.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                        errors.add("Writer " + idx + ": HTTP " + resp.statusCode() + " - " + resp.body());
                        log.warn("❌ Writer {} → HTTP {} | body: {}", idx, resp.statusCode(), resp.body());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    errors.add("Writer " + idx + ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
                    log.warn("❌ Writer {} 예외: {}", idx, e.getMessage());
                } finally {
                    done.countDown();
                }
            });
        }

        // Reader 스레드 (GET)
        for (int i = 0; i < readers; i++) {
            final int idx = i;
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    HttpResponse<String> resp = httpClient.send(
                            buildGetRequest(),
                            HttpResponse.BodyHandlers.ofString());
                    if (resp.statusCode() == 200) {
                        readSuccess.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                        errors.add("Reader " + idx + ": HTTP " + resp.statusCode() + " - " + resp.body());
                        log.warn("❌ Reader {} → HTTP {} | body: {}", idx, resp.statusCode(), resp.body());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    errors.add("Reader " + idx + ": " + e.getClass().getSimpleName() + " - " + e.getMessage());
                    log.warn("❌ Reader {} 예외: {}", idx, e.getMessage());
                } finally {
                    done.countDown();
                }
            });
        }

        boolean allReady = ready.await(15, TimeUnit.SECONDS);
        assertThat(allReady).as("모든 스레드가 15초 내 준비 완료되어야 함").isTrue();

        long startTime = System.currentTimeMillis();
        start.countDown(); // 🏁 일제히 시작!

        boolean allDone = done.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - startTime;
        executor.shutdown();

        log.info("=".repeat(60));
        log.info("📊 결과 - 쓰기 성공: {}/{}, 읽기 성공: {}/{}, 실패: {}, 소요: {}ms",
                writeSuccess.get(), writers, readSuccess.get(), readers, failCount.get(), elapsed);
        if (!errors.isEmpty()) {
            log.warn("⚠️ 오류 샘플 (최대 10건):");
            errors.stream().limit(10).forEach(e -> log.warn("  {}", e));
        }
        log.info("=".repeat(60));

        assertThat(allDone)
                .as("⚠️ %d초 초과! 핀닝 또는 풀 고갈 의심", TIMEOUT_SECONDS)
                .isTrue();
        assertThat(failCount.get())
                .as("실패 건수는 0이어야 함. 오류 (샘플): %s", errors.stream().limit(5).toList())
                .isZero();
        assertThat(writeSuccess.get()).isEqualTo(writers);
        assertThat(readSuccess.get()).isEqualTo(readers);

        long savedCount = countByIdJdbc(currentTestIds);
        assertThat(savedCount)
                .as("DB 저장 수(%d)가 HTTP 쓰기 성공 수(%d)와 일치해야 함", savedCount, writeSuccess.get())
                .isEqualTo(writeSuccess.get());

        log.info("✅ 읽기/쓰기 혼합 동시성 테스트 통과! 소요: {}ms", elapsed);
    }

    // ── 공통 결과 출력 ────────────────────────────────────────────────────────

    private void printResult(String endpoint, int success, int fail, long elapsed, List<String> errors) {
        log.info("=".repeat(60));
        log.info("📊 [{}] 성공: {}, 실패: {}, 소요: {}ms", endpoint, success, fail, elapsed);
        if (!errors.isEmpty()) {
            log.warn("⚠️ 오류 샘플 (최대 10건):");
            errors.stream().limit(10).forEach(e -> log.warn("  {}", e));
        }
        log.info("=".repeat(60));
    }

    /**
     * JDBC native SQL 로 id 리스트에 해당하는 rooms 행 수를 세는 헬퍼.
     * JPA/Hibernate 트랜잭션 락 없이 DataSource 직접 조회.
     */
    private long countByIdJdbc(List<Long> ids) {
        if (ids.isEmpty())
            return 0L;
        String inClause = ids.stream()
                .map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(","));
        String sql = "SELECT COUNT(*) FROM rooms WHERE id IN (" + inClause + ")";
        try (java.sql.Connection conn = dataSource.getConnection();
                java.sql.PreparedStatement ps = conn.prepareStatement(sql);
                java.sql.ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (Exception e) {
            log.error("❌ countByIdJdbc 실패: {}", e.getMessage());
            return -1L;
        }
    }
}
