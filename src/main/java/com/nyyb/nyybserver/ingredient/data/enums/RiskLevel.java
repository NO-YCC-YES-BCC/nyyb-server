package com.nyyb.nyybserver.ingredient.data.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RiskLevel {

    LOW(Description.CAUTION),
    MEDIUM(Description.CAUTION),
    HIGH(Description.CAUTION),
    DISALLOWED(Description.DISALLOWED);

    // 성분 테이블의 description 대신 응답에 내려주는 고정 안내 문구
    private final String description;

    private static class Description {
        private static final String CAUTION = "식품의약품안전처 자료에 근거해 사용 주의가 필요한 성분이에요.";
        private static final String DISALLOWED = "식품의약품안전처 자료에 근거해 사용금지된 성분이에요.";
    }
}
