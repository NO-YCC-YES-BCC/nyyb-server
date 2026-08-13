package com.nyyb.nyybserver.routine.data.dto.response;

public record RoutineProductDto(
        Long id,            // productId
        String imageUrl,    // imageKey로 발급한 이미지 URL
        String productName
) {
}
