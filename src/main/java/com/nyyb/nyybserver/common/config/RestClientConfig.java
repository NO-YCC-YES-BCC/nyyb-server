package com.nyyb.nyybserver.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient clovaOcrRestClient(
            @Value("${clova-ocr.api-url}") String apiUrl
    ) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(Duration.ofSeconds(3))   // 연결 수립 대기
                .withReadTimeout(Duration.ofSeconds(10));     // 응답(읽기) 대기

        ClientHttpRequestFactory requestFactory =
                ClientHttpRequestFactoryBuilder.detect().build(settings);

        return RestClient.builder()
                .baseUrl(apiUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
