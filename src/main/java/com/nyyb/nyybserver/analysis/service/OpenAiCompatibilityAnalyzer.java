package com.nyyb.nyybserver.analysis.service;

import com.nyyb.nyybserver.analysis.data.dto.response.LlmCompatibilityResponseDto;
import com.nyyb.nyybserver.analysis.data.exception.CompatibilityApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OpenAiCompatibilityAnalyzer implements CompatibilityAnalyzer {

    private final ChatClient compatibilityChatClient;

    public OpenAiCompatibilityAnalyzer(
            @Qualifier("compatibilityChatClient") ChatClient compatibilityChatClient
    ) {
        this.compatibilityChatClient = compatibilityChatClient;
    }

    @Override
    public LlmCompatibilityResponseDto analyze(String userMessage) {
        try {
            LlmCompatibilityResponseDto response = compatibilityChatClient.prompt()
                    .user(userMessage)
                    .call()
                    .entity(LlmCompatibilityResponseDto.class);

            if (response == null) {
                throw new CompatibilityApiException();
            }
            return response;
        } catch (CompatibilityApiException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("OpenAI 궁합 분석 호출 중 오류가 발생했습니다.", e);
            throw new CompatibilityApiException();
        }
    }
}
