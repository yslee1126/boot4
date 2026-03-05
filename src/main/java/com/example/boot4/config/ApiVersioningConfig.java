package com.example.boot4.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.accept.ApiVersionStrategy;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring Framework 7 (Boot 4) 네이티브 API Versioning 설정
 *
 * 전략: X-API-Version 헤더 기반 버전 라우팅
 * - 헤더 없이 요청 시 defaultVersion(1.0) 으로 처리
 * - SemanticApiVersionParser 가 "1.0", "2.0" 형태의 버전 파싱 담당
 *
 * buildApiVersionStrategy() 는 테스트에서 MockMvc 에 직접 전략을 주입할 때 사용한다.
 * ApiVersionConfigurer.getApiVersionStrategy() 가 protected 이므로
 * 내부 서브클래스(ExposedApiVersionConfigurer)로 접근 권한을 우회한다.
 */
@Configuration
public class ApiVersioningConfig implements WebMvcConfigurer {

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        applyStrategy(configurer);
    }

    /**
     * 테스트에서 MockMvc 에 직접 주입하기 위한 ApiVersionStrategy 빌드 메서드.
     */
    public ApiVersionStrategy buildApiVersionStrategy() {
        ExposedApiVersionConfigurer configurer = new ExposedApiVersionConfigurer();
        applyStrategy(configurer);
        return configurer.getApiVersionStrategy();
    }

    private void applyStrategy(ApiVersionConfigurer configurer) {
        configurer
                .useRequestHeader("X-API-Version") // 헤더 기반 버전 추출
                .setDefaultVersion("1.0") // 버전 헤더 없으면 1.0 으로 fallback
                .setVersionRequired(false); // 버전 헤더 없어도 오류 X
    }

    /**
     * ApiVersionConfigurer.getApiVersionStrategy() 가 protected 이므로
     * 서브클래스를 통해 외부에서 접근 가능하게 한다.
     */
    private static class ExposedApiVersionConfigurer extends ApiVersionConfigurer {
        @Override
        public ApiVersionStrategy getApiVersionStrategy() {
            return super.getApiVersionStrategy();
        }
    }
}
