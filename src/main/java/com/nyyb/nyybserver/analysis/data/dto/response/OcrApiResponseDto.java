package com.nyyb.nyybserver.analysis.data.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OcrApiResponseDto(
        List<Image> images
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Image(
            List<Field> fields
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Field(
            String inferText,
            boolean lineBreak
    ) {}
}
