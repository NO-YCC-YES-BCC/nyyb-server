package com.nyyb.nyybserver.mypage.data.dto.response;

/**
 * 마이페이지 상단 통계 카드.
 * @param usingProductCount  가장 최신 루틴에 담긴 제품 수 (사용하는 제품)
 * @param removedProductCount 가장 최신 루틴에서 덜어낸 제품 수 (beforeCount - afterCount)
 * @param analysisCount      해당 유저의 누적 분석 횟수
 */
public record MyPageResponseDto(
        long usingProductCount,
        long removedProductCount,
        long analysisCount
) {
}
