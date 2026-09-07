package com.nyyb.nyybserver.common.config;

import com.nyyb.nyybserver.analysis.data.dto.response.LlmAnalysisResponseDto;
import com.nyyb.nyybserver.analysis.data.dto.response.LlmCompatibilityResponseDto;
import com.nyyb.nyybserver.product.data.dto.response.LlmProductIngredientsDto;
import com.nyyb.nyybserver.routine.data.dto.response.LlmRoutineResponseDto;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionRequest.WebSearchOptions;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionRequest.WebSearchOptions.SearchContextSize;
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

    @Value("classpath:prompts/product-ingredient-system-prompt.st")
    private Resource productIngredientSystemPrompt;

    // 웹서치를 지원하는 모델만 web_search_options를 받는다. (기본 채팅 모델과 별개)
    @Value("${openai.product-ingredient.model:gpt-5-search-api}")
    private String productIngredientModel;

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
     * 전성분 조회 전용 클라이언트. OpenAI 내장 웹서치를 켜서 모델이 직접 검색·인용하게 한다.
     * 웹서치 모델은 temperature 같은 옵션을 받지 않으므로, ChatClient에 옵션을 얹는 대신
     * 모델의 기본 옵션 자체를 갈아끼워 application.yml 기본값이 섞여 들어가지 않게 한다.
     */
    @Bean
    public ChatClient productIngredientChatClient(OpenAiChatModel openAiChatModel) {
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(productIngredientModel)
                .webSearchOptions(new WebSearchOptions(SearchContextSize.MEDIUM, null))
                .responseFormat(strictJsonSchema("product_ingredients", LlmProductIngredientsDto.class))
                .build();

        OpenAiChatModel searchModel = openAiChatModel.mutate()
                .defaultOptions(options)
                .build();

        return ChatClient.builder(searchModel)
                .defaultSystem(productIngredientSystemPrompt)
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
        return OpenAiChatOptions.builder()
                .responseFormat(strictJsonSchema(name, responseType))
                .build();
    }

    private ResponseFormat strictJsonSchema(String name, Class<?> responseType) {
        return ResponseFormat.builder()
                .type(ResponseFormat.Type.JSON_SCHEMA)
                .jsonSchema(ResponseFormat.JsonSchema.builder()
                        .name(name)
                        .schema(new BeanOutputConverter<>(responseType).getJsonSchemaMap())
                        .strict(true)
                        .build())
                .build();
    }
}
