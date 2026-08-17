package com.nyyb.nyybserver.report.controller;

import com.nyyb.nyybserver.common.response.GlobalResponse;
import com.nyyb.nyybserver.common.security.SecurityUtil;
import com.nyyb.nyybserver.report.data.dto.response.ReportResponseDto;
import com.nyyb.nyybserver.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/reports")
@Tag(name = "Report", description = "Routine report APIs")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/latest")
    @Operation(summary = "Get the current user's latest report")
    public GlobalResponse<ReportResponseDto> getLatestReport() {
        return GlobalResponse.ok(reportService.getLatestReport(SecurityUtil.getUserId()));
    }

    @GetMapping("/{reportId}")
    @Operation(summary = "Get a routine report")
    public GlobalResponse<ReportResponseDto> getReport(@PathVariable UUID reportId) {
        return GlobalResponse.ok(reportService.getReport(reportId, SecurityUtil.getUserId()));
    }
}
