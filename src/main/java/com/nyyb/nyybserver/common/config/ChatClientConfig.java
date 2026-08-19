package com.nyyb.nyybserver.common.config;

import com.nyyb.nyybserver.analysis.data.dto.response.LlmAnalysisResponseDto;
import com.nyyb.nyybserver.analysis.data.dto.response.LlmCompatibilityResponseDto;
import com.nyyb.nyybserver.routine.data.dto.response.LlmRoutineResponseDto;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
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

    @Value("classpath:prompts/compatibility-system-prompt.st")
    private Resource compatibilitySystemPrompt;

    // 빈 이름(chatClient / routineChatClient)을 주입 필드명과 맞춰 구분 주입한다.
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem(analysisSystemPrompt)
                .defaultOptions(strictJsonOptions("product_analysis", LlmAnalysisResponseDto.class))
                .build();
    }

    @Bean
    public ChatClient routineChatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem(routineSystemPrompt)
                .defaultOptions(strictJsonOptions("routine_design", LlmRoutineResponseDto.class))
                .build();
    }

    @Bean
    public ChatClient compatibilityChatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem(compatibilitySystemPrompt)
                .defaultOptions(strictJsonOptions("compatibility_analysis", LlmCompatibilityResponseDto.class))
                .build();
    }

    /**
     * 응답 DTO에서 뽑은 JSON 스키마를 strict 구조화 출력으로 거는 옵션.
     * .entity(...)만 쓰면 JSON 형식이 프롬프트 지시로만 전달돼 모델이 이유 문구 등을 누락해도 막을 수 없지만,
     * strict 스키마는 제약된 디코딩으로 걸리므로 필드 누락·null 응답 자체를 만들 수 없다.
     * model·temperature 등 나머지 옵션은 비워 두면 application.yml 기본값이 그대로 병합된다.
     * @param name         스키마 이름 (OpenAI 요청 식별용)
     * @param responseType 구조화 출력으로 받을 응답 DTO 타입
     */
    private OpenAiChatOptions strictJsonOptions(String name, Class<?> responseType) {
        ResponseFormat responseFormat = ResponseFormat.builder()
                .type(ResponseFormat.Type.JSON_SCHEMA)
                .jsonSchema(ResponseFormat.JsonSchema.builder()
                        .name(name)
                        .schema(new BeanOutputConverter<>(responseType).getJsonSchemaMap())
                        .strict(true)
                        .build())
                .build();

        return OpenAiChatOptions.builder()
                .responseFormat(responseFormat)
                .build();
    }
}
