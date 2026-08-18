package com.nyyb.nyybserver.analysis.data.dto.response;

import java.util.List;

/**
 * 새 제품과 성분이 중복되는 현재 루틴 제품을 화면의 중복 카드로 전달한다.
 *
 * @param productCount 성분이 하나 이상 중복되는 루틴 제품 수
 * @param summary 중복 카드 하단에 표시할 요약 문구
 * @param products 제품별 중복 성분 상세
 */
public record CompatibilityDuplicateReportDto(
        int productCount,
        String summary,
        List<CompatibilityIssueDto> products
) {
}
