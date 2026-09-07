package com.nyyb.nyybserver.product.data.dto.response;

import java.util.List;

/**
 * LLM이 찾아 온 전성분을 성분 마스터와 매칭한 결과.
 * matched의 rawName(LLM 표기)과 name(마스터 대표 성분명)이 다르면 이명 테이블로 걸린 것이다.
 */
public record ProductIngredientMappingDto(
        Long productId,
        String itemName,
        boolean found,          // LLM이 전성분 근거를 찾았는지
        String source,          // LLM이 근거로 삼은 출처
        int totalCount,         // LLM이 돌려준 성분 수
        int matchedCount,       // 성분 마스터에 매칭된 수
        List<MatchedIngredient> matched,
        List<String> unmatched  // 마스터에 없어 매칭 실패한 성분 표기
) {
    public record MatchedIngredient(
            Long ingredientId,
            String name,    // 성분 마스터의 대표 이름
            String rawName  // LLM이 돌려준 표기 (이명일 수 있다)
    ) {
    }
}
