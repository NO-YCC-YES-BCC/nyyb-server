package com.nyyb.nyybserver.report.data.dto.response;

import com.nyyb.nyybserver.ingredient.data.enums.RiskLevel;

public record ReportIngredientDto(
        String name,
        boolean matched,
        RiskLevel riskLevel,
        boolean toxic,
        String description
) {
}
