package com.nyyb.nyybserver.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient clovaOcrRestClient(
            @Value("${clova-ocr.api-url}") String apiUrl
    ) {
        return RestClient.builder()
                .baseUrl(apiUrl)
                .build();
    }
}
