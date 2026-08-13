package com.nyyb.nyybserver.analysis.controller;

import com.nyyb.nyybserver.analysis.data.dto.request.AnalysisRequestDto;
import com.nyyb.nyybserver.analysis.data.dto.response.AnalysisResponseDto;
import com.nyyb.nyybserver.analysis.data.dto.response.OcrResponseDto;
import com.nyyb.nyybserver.analysis.service.AnalysisService;
import com.nyyb.nyybserver.common.response.GlobalResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/analyses")
@Tag(name = "Analysis", description = "OCR and analysis APIs")
public class AnalysisController {

    private final AnalysisService analysisService;

    @PostMapping(value = "/ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public GlobalResponse<OcrResponseDto> ocr(@RequestPart("file") MultipartFile file) {
        return GlobalResponse.ok(analysisService.ocr(file));
    }

    @PostMapping
    public GlobalResponse<AnalysisResponseDto> analyze(@RequestBody AnalysisRequestDto request) {
        return GlobalResponse.ok(analysisService.analyze(request));
    }
}
