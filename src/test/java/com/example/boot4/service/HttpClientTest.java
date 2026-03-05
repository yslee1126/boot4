package com.example.boot4.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Subtask;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class HttpClientTest {

    @Autowired
    private HttpClient httpClient;

    @Test
    @DisplayName("토스 기술 블로그 RSS 피드 가져오기 테스트")
    void getTossRssFeed() {
        String feed = httpClient.getTossRssFeed();
        assertThat(feed).isNotEmpty();
        assertThat(feed).contains("rss");
        System.out.println("Toss Feed Length: " + feed.length());
    }

    @Test
    @DisplayName("네이버 D2 기술 블로그 Atom 피드 가져오기 테스트")
    void getD2RssFeed() {
        String feed = httpClient.getD2RssFeed();
        assertThat(feed).isNotEmpty();
        assertThat(feed).contains("feed");
        System.out.println("Naver D2 Feed Length: " + feed.length());
    }

    @Test
    @DisplayName("Structured Concurrency를 이용한 병렬 피드 가져오기 테스트 (JDK 25 style)")
    void getParallelFeedsWithStructuredConcurrency() throws InterruptedException {
        long startTime = System.currentTimeMillis();

        // JDK 25에서는 생성자 대신 정적 팩토리 메서드 open()을 사용합니다.
        // 기본적으로 모든 작업이 성공하거나 하나가 실패할 때까지 대기하는 정책이 적용됩니다.
        try (var scope = StructuredTaskScope.open()) {
            // 두 작업을 병렬로 실행(fork)
            var tossSubtask = scope.fork(httpClient::getTossRssFeed);
            var d2Subtask = scope.fork(httpClient::getD2RssFeed);

            // 모든 작업이 끝날 때까지 대기하며, 실패 시 예외를 직접 전파합니다.
            scope.join();

            // 결과 확인
            String tossFeed = tossSubtask.get();
            String d2Feed = d2Subtask.get();

            assertThat(tossFeed).isNotEmpty();
            assertThat(d2Feed).isNotEmpty();

            long duration = System.currentTimeMillis() - startTime;
            System.out.println("Parallel Fetch Completed!");
            System.out.println("Toss Feed Length: " + tossFeed.length());
            System.out.println("Naver D2 Feed Length: " + d2Feed.length());
            System.out.println("Total duration: " + duration + "ms");
        } catch (Exception e) {
            System.err.println("Task failed: " + e.getMessage());
            throw e;
        }
    }
}
