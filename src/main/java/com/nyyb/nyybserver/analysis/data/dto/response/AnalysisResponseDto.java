package com.nyyb.nyybserver.analysis.data.dto.response;

import java.util.List;
import java.util.UUID;

public record AnalysisResponseDto(
        UUID routineId,                       // analyze 단계에서 생성된 루틴 id (designRoutine 호출용)
        String title,                         // 한국 날짜 + 제품 개수 문구 (예: "8월 3일 5개의 제품")
        List<LlmProductAnalysisDto> products  // 제품별 KEEP/REMOVE 분석 결과
) {
}
