package com.nyyb.nyybserver.analysis.data.dto.request;

import java.util.UUID;

/**
 * OCR로 저장된 구매 예정 제품과 비교할 루틴을 지정한다.
 * productId는 POST /analyses/ocr 응답에서 받은 값을 사용한다.
 */
public record CompatibilityRequestDto(
        Long productId,
        UUID routineId
) {
}
