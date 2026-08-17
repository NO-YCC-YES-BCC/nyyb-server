package com.nyyb.nyybserver.analysis.service;

import com.nyyb.nyybserver.analysis.data.dto.response.LlmCompatibilityResponseDto;

public interface CompatibilityAnalyzer {

    LlmCompatibilityResponseDto analyze(String userMessage);
}
