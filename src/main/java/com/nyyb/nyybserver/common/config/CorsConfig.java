package com.nyyb.nyybserver.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    /**
     * 허용할 프론트 오리진. 기본값 "*" 는 검증 페이지(비로그인)용이며,
     * 운영에서는 cors.allowed-origins 로 실제 도메인만 지정하는 것을 권장한다.
     * 예) cors.allowed-origins=https://sott.example.com,http://localhost:5500
     */
    @Value("${cors.allowed-origins:*}")
    private List<String> allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // allowedOriginPatterns 는 "*" 와 file:// 페이지가 보내는 Origin: null 까지 처리한다
        config.setAllowedOriginPatterns(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        // 쿠키/세션을 쓰지 않는다(JWT는 Authorization 헤더). credentials 를 켜면 "*" 오리진을 못 쓴다.
        config.setAllowCredentials(false);

        // preflight(OPTIONS) 결과 캐시 — 매 요청마다 preflight 가 나가지 않게
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
