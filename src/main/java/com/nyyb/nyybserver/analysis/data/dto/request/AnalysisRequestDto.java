package com.nyyb.nyybserver.analysis.data.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class AnalysisRequestDto {

    private List<Long> productsIds;
}
