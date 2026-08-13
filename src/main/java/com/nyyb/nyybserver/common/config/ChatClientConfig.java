package com.nyyb.nyybserver.common.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
public class ChatClientConfig {

    @Value("classpath:prompts/analysis-system-prompt.st")
    private Resource analysisSystemPrompt;

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem(analysisSystemPrompt)
                .build();
    }
}
