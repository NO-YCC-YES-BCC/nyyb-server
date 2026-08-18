package com.nyyb.nyybserver.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class CorsConfig {

    /**
     * 허용할 프론트 오리진. 로컬 개발값을 기본으로 두고, 배포 환경에서는
     * CORS_ALLOWED_ORIGINS 환경변수로 덮어쓴다.
     * 예) CORS_ALLOWED_ORIGINS=https://*.vercel.app,https://nyyb.example.com
     */
    @Value("${cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
    private List<String> allowedOrigins;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        // 와일드카드(*.vercel.app)를 쓰려면 allowedOriginPatterns 여야 한다
                        .allowedOriginPatterns(allowedOrigins.toArray(new String[0]))
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .exposedHeaders("Authorization")
                        .allowCredentials(false);
            }
        };
    }
}
