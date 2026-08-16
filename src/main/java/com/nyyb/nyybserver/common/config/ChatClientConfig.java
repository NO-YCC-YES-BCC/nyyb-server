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

    @Value("classpath:prompts/routine-system-prompt.st")
    private Resource routineSystemPrompt;

    // 빈 이름(chatClient / routineChatClient)을 주입 필드명과 맞춰 구분 주입한다.
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem(analysisSystemPrompt)
                .build();
    }

    @Bean
    public ChatClient routineChatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem(routineSystemPrompt)
                .build();
    }
}
