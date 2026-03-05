package com.example.boot4.config;

import com.example.boot4.service.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class HttpClientConfig {

    @Bean
    public HttpClient httpClient() {
        // RestClient를 생성합니다.
        // 인터페이스의 각 메서드가 절대 경로를 사용하므로 여기서는 baseUrl을 생략하거나 기본값을 둘 수 있습니다.
        RestClient restClient = RestClient.builder().build();

        // HttpServiceProxyFactory를 통해 @HttpExchange 인터페이스를 구현한 프록시 객체를 생성합니다.
        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();

        return factory.createClient(HttpClient.class);
    }
}
