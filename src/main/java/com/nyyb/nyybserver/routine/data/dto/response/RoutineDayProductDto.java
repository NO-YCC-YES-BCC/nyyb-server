package com.nyyb.nyybserver.routine.data.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nyyb.nyybserver.analysis.data.enums.RoutineRecommendStatus;

// recommended가 해당 시간대에 해당할 때만 recommended·recommendReason 포함(그 외엔 생략)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RoutineDayProductDto(
        Long id,                             // productId
        String imageUrl,                     // imageKey로 발급
        String productName,
        RoutineRecommendStatus recommended,  // 이 시간대에 해당할 때만, 아니면 생략
        String recommendReason               // 이 시간대에 해당할 때만, 아니면 생략
) {
}
