package com.nyyb.nyybserver.analysis.controller;

import com.nyyb.nyybserver.analysis.data.dto.response.OcrResponseDto;
import com.nyyb.nyybserver.analysis.service.AnalysisService;
import com.nyyb.nyybserver.common.response.GlobalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/analyses")
@Tag(name = "Analysis", description = "OCR and analysis APIs")
public class AnalysisController {

    private final AnalysisService analysisService;

    @Operation
    @PostMapping(value = "/ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public GlobalResponse<OcrResponseDto> ocr(@RequestPart("file") MultipartFile file) {
        return GlobalResponse.ok(analysisService.requestOcr(file));
    }
}
