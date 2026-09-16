package com.nyyb.nyybserver.product.data.dto.response;

/**
 * 검색창 자동완성 항목. 입력할 때마다 호출되는 자리라 목록에 띄울 이름과,
 * 사용자가 고른 뒤 리포트 요청에 그대로 실어 보낼 제품 id만 담는다.
 * (브랜드·카테고리까지 필요하면 {@link ProductSearchDto}를 쓰는 검색 API를 호출한다)
 */
public record ProductSuggestionDto(
        Long productId, // 제품 마스터 id
        String name     // 제품명 (한글 제품명 우선)
) {
}
