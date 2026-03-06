package com.example.boot4.config;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;

/**
 * OpenTelemetry 수동 설정
 *
 * ============================================================================
 * OpenTelemetry 활성화 방법 (기본: 비활성화)
 * ============================================================================
 *
 * application.yml에 다음 설정 추가:
 *
 *   management:
 *     tracing:
 *       enabled: true
 *
 * 또는 VM 옵션:
 *   -Dmanagement.tracing.enabled=true
 *
 * ============================================================================
 *
 * Spring Boot 4.0 + Actuator가 있어도 완전한 자동 설정이 안 되는 부분:
 * - OTLP exporter 설정이 제대로 동작하지 않음
 * - TracingObservationHandler가 ObservationRegistry에 자동 등록되지 않음
 *
 * 따라서 아래 5개 bean을 수동으로 정의해야 trace가 Grafana로 전송됨
 *
 * ============================================================================
 * 데이터 흐름도 (Trace가 Grafana까지 전달되는 과정)
 * ============================================================================
 *
 * 1. HTTP 요청 발생
 *    ↓
 * 2. ObservationRegistry (Spring Boot Actuator가 HTTP 요청 자동 감지)
 *    ↓
 * 3. TracingObservationHandler (Observation → Span 변환) ← 핵심!
 *    ↓
 * 4. Tracer (Micrometer ↔ OpenTelemetry 브릿지)
 *    ↓
 * 5. OpenTelemetry SDK (전역 인스턴스)
 *    ↓
 * 6. BatchSpanProcessor (span을 배치로 모아서 비동기 처리)
 *    ↓
 * 7. OtlpGrpcSpanExporter (gRPC로 직렬화)
 *    ↓
 * 8. Grafana Tempo (http://localhost:4317)
 *
 * ============================================================================
 */
@Configuration
@ConditionalOnProperty(name = "management.tracing.enabled", havingValue = "true", matchIfMissing = false)
public class OtelConfig {

    @Value("${spring.application.name:boot4}")
    private String serviceName;

    @Value("${management.otlp.tracing.endpoint:http://localhost:4317}")
    private String otlpEndpoint;

    /**
     * OpenTelemetry SDK 설정
     *
     * 역할:
     * - Resource: 서비스 이름 등 메타데이터 설정
     * - OtlpGrpcSpanExporter: Grafana Tempo로 trace 전송 (gRPC)
     * - BatchSpanProcessor: span을 배치로 모아 비동기 전송 (성능 최적화)
     * - buildAndRegisterGlobal(): 전역 OpenTelemetry 인스턴스로 등록
     */
    @Bean
    public OpenTelemetry openTelemetry() {
        Resource resource = Resource.getDefault()
                .merge(Resource.builder().put(SERVICE_NAME, serviceName).build());

        OtlpGrpcSpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
                .setEndpoint(otlpEndpoint)
                .build();

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .setResource(resource)
                .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
                .build();

        return OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .buildAndRegisterGlobal();
    }

    /**
     * Micrometer Tracer ↔ OpenTelemetry 브릿지
     *
     * 역할:
     * - OtelTracer: Micrometer의 Tracer 인터페이스를 OpenTelemetry로 구현
     * - OtelCurrentTraceContext: 현재 실행 중인 trace context 관리
     *
     * 이 bean이 있어야 Controller에서 Tracer 주입 받아 currentSpan() 사용 가능
     */
    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        io.opentelemetry.api.trace.Tracer otelTracer = openTelemetry.getTracer(serviceName);
        OtelCurrentTraceContext traceContext = new OtelCurrentTraceContext();
        return new OtelTracer(otelTracer, traceContext, event -> {});
    }

    /**
     * @Observed 어노테이션 지원
     *
     * 역할:
     * - @Observed가 붙은 메서드를 AOP로 감지
     * - 메서드 실행 전/후로 Observation 생성
     *
     * Controller의 @Observed(name = "otel.test.request") 처리
     */
    @Bean
    public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
        return new ObservedAspect(observationRegistry);
    }

    /**
     * Observation → Trace 변환 핸들러 (핵심!)
     *
     * 역할:
     * - Micrometer의 Observation 이벤트를 받아서
     * - OpenTelemetry의 Span으로 변환
     *
     * 이게 없으면 Observation이 발생해도 trace가 생성되지 않음
     */
    @Bean
    public io.micrometer.tracing.handler.TracingObservationHandler<?> tracingObservationHandler(Tracer tracer) {
        return new io.micrometer.tracing.handler.DefaultTracingObservationHandler(tracer);
    }

    /**
     * Observation 중앙 레지스트리
     *
     * 역할:
     * - TracingObservationHandler를 등록
     * - HTTP 요청, @Observed 메서드 등 모든 observation 이벤트를 받아 handler로 전달
     *
     * Spring Boot가 ObservationRegistry는 만들지만 TracingObservationHandler를
     * 자동으로 등록하지 않아서 수동 설정 필요
     *
     * 데이터 흐름:
     * HTTP 요청 → ObservationRegistry → TracingObservationHandler → Tracer
     * → OpenTelemetry SDK → BatchSpanProcessor → OtlpHttpSpanExporter → Grafana Tempo
     */
    @Bean
    public ObservationRegistry observationRegistry(
            io.micrometer.tracing.handler.TracingObservationHandler<?> tracingHandler) {
        ObservationRegistry registry = ObservationRegistry.create();
        registry.observationConfig().observationHandler(tracingHandler);
        return registry;
    }
}
