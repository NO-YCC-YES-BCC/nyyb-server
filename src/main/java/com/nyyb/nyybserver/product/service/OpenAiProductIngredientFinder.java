package com.nyyb.nyybserver.product.service;

import com.nyyb.nyybserver.product.data.dto.response.LlmProductIngredientsDto;
import com.nyyb.nyybserver.product.data.exception.ProductIngredientApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OpenAiProductIngredientFinder implements ProductIngredientFinder {

    private final ChatClient productIngredientChatClient;

    public OpenAiProductIngredientFinder(
            @Qualifier("productIngredientChatClient") ChatClient productIngredientChatClient
    ) {
        this.productIngredientChatClient = productIngredientChatClient;
    }

    @Override
    public LlmProductIngredientsDto find(String itemName) {
        try {
            LlmProductIngredientsDto response = productIngredientChatClient.prompt()
                    .user("제품명: " + itemName)
                    .call()
                    .entity(LlmProductIngredientsDto.class);

            if (response == null) {
                throw new ProductIngredientApiException();
            }
            return response;
        } catch (ProductIngredientApiException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("OpenAI 제품 전성분 조회 호출 중 오류가 발생했습니다. itemName={}", itemName, e);
            throw new ProductIngredientApiException();
        }
    }
}
