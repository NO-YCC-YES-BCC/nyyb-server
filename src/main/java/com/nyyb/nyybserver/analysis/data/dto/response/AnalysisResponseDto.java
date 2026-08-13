package com.nyyb.nyybserver.analysis.data.dto.response;

import java.util.List;
import java.util.UUID;

public record AnalysisResponseDto(
        UUID routineId,                       // analyze 단계에서 생성된 루틴 id (createRoutine 호출용)
        List<LlmProductAnalysisDto> products  // 제품별 KEEP/REMOVE 분석 결과
) {
}
