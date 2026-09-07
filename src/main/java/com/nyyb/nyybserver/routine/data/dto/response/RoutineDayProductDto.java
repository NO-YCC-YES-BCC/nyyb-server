package com.nyyb.nyybserver.routine.data.dto.response;

import com.nyyb.nyybserver.analysis.data.enums.RecommendStatus;

import java.util.UUID;

// 슬롯 정보는 응답 최상단 slot에만 있고, 제품 안에는 KEEP/REMOVE만 담는다.
public record RoutineDayProductDto(
        UUID id,                        // userProductId
        String categoryMain,            // 카테고리 대분류 (한글 라벨)
        String categorySub,             // 카테고리 소분류 (한글 라벨)
        String productName,
        RecommendStatus recommended,    // 이 슬롯 기준 KEEP / REMOVE
        String recommendReason          // LLM 이유 문구
) {
}
