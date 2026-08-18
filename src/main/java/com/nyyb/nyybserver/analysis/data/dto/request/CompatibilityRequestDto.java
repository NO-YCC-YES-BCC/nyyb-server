package com.nyyb.nyybserver.analysis.data.dto.request;

import lombok.Data;

import java.util.UUID;

@Data
public class CompatibilityRequestDto {

    private Long productId;
    private UUID routineId;
}
