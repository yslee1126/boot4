package com.example.boot4.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Scoped Values 실험 - 요청 진입점에서 requestId를 바인딩하는 필터
 *
 * 동작 흐름:
 * 1. X-Request-Id 헤더가 있으면 해당 값을 사용 (upstream 연동용)
 * 2. 없으면 UUID 신규 생성
 * 3. ScopedValue.where().call() 로 requestId를 현재 실행 범위에 바인딩
 * 4. filterChain.doFilter() 호출 → 컨트롤러/서비스가 해당 범위 안에서 실행
 * 5. call() 반환 즉시 바인딩 자동 해제 (ThreadLocal.remove() 불필요!)
 *
 * Java 25 ScopedValue API:
 * - ScopedValue.where(key, value) → Carrier 반환
 * - Carrier.run(Runnable) → checked exception 전파 불가
 * - Carrier.call(CallableOp<R, X>) → throws Exception 선언
 * ※ call()이 throws Exception 이므로 doFilterInternal과 직접 연결 불가
 * → try-catch 로 IOException/ServletException 을 정확히 rethrow 필요
 *
 * Virtual Thread 관점:
 * - ScopedValue는 StructuredTaskScope와 조합 시 자식 VT에도 자동 전파됨
 * - 반면 MDC(ThreadLocal 기반)는 자식 VT에서 값이 비어있을 수 있음
 *
 * OpenTelemetry와의 차이:
 * - OpenTelemetry: 분산 tracing 시스템 (Grafana Tempo로 전송)
 * - RequestId: 단순 요청 식별자 (로그/디버깅용, Java 25 ScopedValue 기능 테스트용)
 */
@Component
@Order(2) // TraceIdFilter(Order=1) 다음에 실행되도록 설정
public class RequestIdFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);
    private static final String REQUEST_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // 1. 헤더에서 requestId 추출, 없으면 신규 생성
        String requestId = request.getHeader(REQUEST_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = "REQ-" + UUID.randomUUID().toString().substring(0, 8);
        }

        // 응답 헤더에도 requestId 추가 (클라이언트에서 추적 가능하도록)
        response.setHeader(REQUEST_HEADER, requestId);

        log.info("[RequestIdFilter] requestId 바인딩 시작 | requestId={} | thread={}",
                requestId, Thread.currentThread());

        // 2. ScopedValue 바인딩
        final String finalRequestId = requestId;
        try {
            ScopedValue.where(RequestContext.REQUEST_ID, finalRequestId).call(() -> {
                filterChain.doFilter(request, response);
                return null;
            });
        } catch (IOException | ServletException e) {
            throw e; // 원래 checked exception 그대로 rethrow
        } catch (Exception e) {
            throw new ServletException("RequestIdFilter: unexpected error", e);
        }

        log.info("[RequestIdFilter] requestId 바인딩 종료 (자동 해제) | requestId={}", finalRequestId);
    }
}
