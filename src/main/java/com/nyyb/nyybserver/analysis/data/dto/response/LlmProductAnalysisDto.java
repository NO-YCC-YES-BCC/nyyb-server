package com.nyyb.nyybserver.analysis.data.dto.response;

import com.nyyb.nyybserver.analysis.data.enums.RecommendStatus;

public record LlmProductAnalysisDto(
        Long productId,
        String productName,
        RecommendStatus recommended, // KEEP / REMOVE
        String recommendReason
) {
}
