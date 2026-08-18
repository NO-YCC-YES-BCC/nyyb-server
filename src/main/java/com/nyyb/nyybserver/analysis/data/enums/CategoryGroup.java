package com.nyyb.nyybserver.analysis.data.enums;

import lombok.Getter;

/**
 * 제품 카테고리를 루틴에서의 역할 단위로 묶은 기능 그룹.
 * 같은 그룹 안의 제품끼리만 역할 중복을 비교하고, 그룹마다 최소 1개는 남기도록
 * LLM 요청 메시지(analysis·routine)에 카테고리와 함께 실어 보낸다.
 */
@Getter
public enum CategoryGroup {

    CLEANSING("세안"),      // 씻어내는 단계 (클렌징 오일·워터·폼, 각질 케어)
    SKIN_CARE("피부관리"),   // 바르고 흡수시키는 단계 (토너·에센스·세럼·크림 등)
    PROTECTION("피부보호"),  // 외부 자극 차단 단계 (선케어)
    ADDITIONAL("부가제품"),  // 매일 상시 사용하지 않는 단계 (마스크·국소 케어·립케어)
    ETC("기타");            // 그룹 판별이 어려운 제품

    private final String korName;

    CategoryGroup(String korName) {
        this.korName = korName;
    }
}
