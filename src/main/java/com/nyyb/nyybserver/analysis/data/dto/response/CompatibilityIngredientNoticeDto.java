package com.nyyb.nyybserver.analysis.data.dto.response;

import com.nyyb.nyybserver.ingredient.data.enums.RiskLevel;

/**
 * 구매 예정 제품에서 확인된 식품의약품안전처 기준 정보이다.
 * 화면에서 임의로 해석하지 않도록 표시 문구까지 백엔드에서 완성해 전달한다.
 */
public record CompatibilityIngredientNoticeDto(
        Long ingredientId,
        String name,
        RiskLevel riskLevel,
        boolean registeredAllergen,
        String source,
        String allergenNotice,
        String allergenThresholdNotice,
        String regulatoryNotice,
        String description,
        String informationDisclaimer
) {
}
