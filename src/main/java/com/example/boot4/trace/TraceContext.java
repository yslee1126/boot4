package com.example.boot4.trace;

/**
 * Scoped Values 실험 - traceId 전파 컨텍스트
 *
 * ScopedValue는 Java 21+ preview / Java 25 정식(예정) 기능으로,
 * ThreadLocal의 단점을 해소하는 불변(immutable) 범위 변수이다.
 *
 * ThreadLocal과의 주요 차이:
 * - ThreadLocal : 스레드에 종속, 명시적 remove() 필요, Virtual Thread 자식 미전파
 * - ScopedValue : 실행 범위(scope)에 종속, 범위 종료 시 자동 해제, 불변
 *
 * 사용 방법:
 * // 쓰기 (필터 등 진입점에서 한 번만)
 * ScopedValue.runWhere(TraceContext.TRACE_ID, "abc-123", () -> { ... });
 *
 * // 읽기 (컨트롤러 / 서비스 등 범위 내 어디서든)
 * String id = TraceContext.TRACE_ID.get();
 */
public final class TraceContext {

    private TraceContext() {
    }

    /**
     * 요청 단위 traceId를 담는 ScopedValue.
     * 필터(TraceIdFilter)에서 바인딩되고, 요청 처리가 끝나면 자동 해제된다.
     */
    public static final ScopedValue<String> TRACE_ID = ScopedValue.newInstance();
}
