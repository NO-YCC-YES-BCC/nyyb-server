package com.nyyb.nyybserver.routine.data.dto.response;

import java.util.UUID;

public record RoutineProductDto(
        UUID id,                // userProductId
        String categoryMain,    // 카테고리 대분류 (한글 라벨)
        String categorySub,     // 카테고리 소분류 (한글 라벨)
        String productName
) {
}
