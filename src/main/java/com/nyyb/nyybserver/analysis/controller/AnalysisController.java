package com.nyyb.nyybserver.analysis.controller;

import com.nyyb.nyybserver.analysis.data.dto.request.AnalysisRequestDto;
import com.nyyb.nyybserver.analysis.data.dto.request.CompatibilityRequestDto;
import com.nyyb.nyybserver.analysis.data.dto.response.AnalysisResponseDto;
import com.nyyb.nyybserver.analysis.data.dto.response.AnalysisSummaryDto;
import com.nyyb.nyybserver.analysis.data.dto.response.CompatibilityResponseDto;
import com.nyyb.nyybserver.analysis.data.dto.response.OcrResponseDto;
import com.nyyb.nyybserver.analysis.service.AnalysisService;
import com.nyyb.nyybserver.analysis.service.CompatibilityService;
import com.nyyb.nyybserver.analysis.service.OcrService;
import com.nyyb.nyybserver.common.dto.PageRequestDto;
import com.nyyb.nyybserver.common.response.GlobalResponse;
import com.nyyb.nyybserver.common.security.SecurityUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/analyses")
@Tag(name = "Analysis", description = "OCR and analysis APIs")
public class AnalysisController {

    private final OcrService ocrService;
    private final AnalysisService analysisService;
    private final CompatibilityService compatibilityService;

    @PostMapping(value = "/ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public GlobalResponse<OcrResponseDto> ocr(@RequestPart("file") MultipartFile file) {
        return GlobalResponse.ok(ocrService.ocr(file, SecurityUtil.getUserId()));
    }

    @PostMapping
    public GlobalResponse<AnalysisResponseDto> analyze(@RequestBody AnalysisRequestDto request) {
        return GlobalResponse.ok(analysisService.analyze(request, SecurityUtil.getUserId()));
    }

    @PostMapping(value = "/compatibility", consumes = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResponse<CompatibilityResponseDto> compareCompatibility(
            @RequestBody CompatibilityRequestDto request
    ) {
        return GlobalResponse.ok(compatibilityService.compare(
                request.productId(),
                request.routineId(),
                SecurityUtil.getUserId()
        ));
    }

    // 현재 로그인 유저의 분석 목록(id + title) 최신순 조회
    @GetMapping
    public GlobalResponse<List<AnalysisSummaryDto>> getAnalyses(@ParameterObject PageRequestDto pageRequest) {
        return GlobalResponse.ok(analysisService.getAnalyses(SecurityUtil.getUserId(), pageRequest.toPageable()));
    }

    // 분석 상세 조회 (analyze 응답과 동일한 형식)
    @GetMapping("/{analysisId}")
    public GlobalResponse<AnalysisResponseDto> getAnalysis(@PathVariable UUID analysisId) {
        return GlobalResponse.ok(analysisService.getAnalysis(analysisId, SecurityUtil.getUserId()));
    }

    // 분석 삭제 (소유자만 해제하는 소프트 삭제)
    @DeleteMapping("/{analysisId}")
    public GlobalResponse<Void> deleteAnalysis(@PathVariable UUID analysisId) {
        analysisService.deleteAnalysis(analysisId, SecurityUtil.getUserId());
        return GlobalResponse.ok();
    }
}
