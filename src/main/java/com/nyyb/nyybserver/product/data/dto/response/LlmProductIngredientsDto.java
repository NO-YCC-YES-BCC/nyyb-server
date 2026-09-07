package com.nyyb.nyybserver.product.data.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// LLM이 웹에서 찾아 온 제품 전성분. found가 false면 ingredients는 빈 목록이다.
public record LlmProductIngredientsDto(
        @JsonProperty(required = true) String itemName,
        @JsonProperty(required = true) Boolean found,
        @JsonProperty(required = true) List<String> ingredients,
        @JsonProperty(required = true) String source // 근거로 삼은 출처
) {
}
