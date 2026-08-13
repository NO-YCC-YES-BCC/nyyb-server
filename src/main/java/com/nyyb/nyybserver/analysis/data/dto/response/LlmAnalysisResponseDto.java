package com.nyyb.nyybserver.analysis.data.dto.response;

import java.util.List;

public record LlmAnalysisResponseDto(
        List<LlmProductAnalysisDto> products
) {
}
