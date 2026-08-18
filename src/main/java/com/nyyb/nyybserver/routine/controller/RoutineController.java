package com.nyyb.nyybserver.routine.controller;

import com.nyyb.nyybserver.analysis.data.enums.RoutineSlot;
import com.nyyb.nyybserver.common.dto.PageRequestDto;
import com.nyyb.nyybserver.common.response.GlobalResponse;
import com.nyyb.nyybserver.common.security.SecurityUtil;
import com.nyyb.nyybserver.routine.data.dto.request.RoutineSaveRequestDto;
import com.nyyb.nyybserver.routine.data.dto.response.RoutineDesignResponseDto;
import com.nyyb.nyybserver.routine.data.dto.response.RoutineDayResponseDto;
import com.nyyb.nyybserver.routine.data.dto.response.RoutineSummaryDto;
import com.nyyb.nyybserver.routine.service.RoutineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@RestController
@RequiredArgsConstructor
@RequestMapping("/routines")
@Tag(name = "Routine", description = "Routine design and daily routine APIs")
public class RoutineController {

    private final RoutineService routineService;

    // 내 루틴 목록을 최신순으로 조회한다. page와 size를 쿼리 파라미터로 받을 수 있다.
    @GetMapping
    public GlobalResponse<List<RoutineSummaryDto>> getRoutines(@ParameterObject PageRequestDto pageRequest) {
        return GlobalResponse.ok(routineService.getRoutines(SecurityUtil.getUserId(), pageRequest.toPageable()));
    }

    // 현재 로그인 유저의 가장 최신 루틴 상세 조회.
    @GetMapping("/latest")
    public GlobalResponse<RoutineDesignResponseDto> getLatestRoutine() {
        return GlobalResponse.ok(routineService.getLatestRoutine(SecurityUtil.getUserId()));
    }

    // 루틴 상세 조회 (designRoutine 응답과 동일한 형식)
    @GetMapping("/{routineId}")
    public GlobalResponse<RoutineDesignResponseDto> getRoutine(@PathVariable UUID routineId) {
        return GlobalResponse.ok(routineService.getRoutine(routineId, SecurityUtil.getUserId()));
    }

    // 루틴 삭제 (소유자만 해제하는 소프트 삭제)
    @DeleteMapping("/{routineId}")
    public GlobalResponse<Void> deleteRoutine(@PathVariable UUID routineId) {
        routineService.deleteRoutine(routineId, SecurityUtil.getUserId());
        return GlobalResponse.ok();
    }

    @PostMapping("/{routineId}/design")
    @Operation(summary = "LLM 루틴 설계")
    public GlobalResponse<RoutineDesignResponseDto> designRoutine(@PathVariable UUID routineId) {
        return GlobalResponse.ok(routineService.designRoutine(routineId, SecurityUtil.getUserId()));
    }

    // slot은 MORNING 또는 EVENING이며, 해당 시간대에 사용하는 제품과 추천 결과를 반환한다.
    @GetMapping("/{routineId}/day")
    public GlobalResponse<RoutineDayResponseDto> getRoutineDay(@PathVariable UUID routineId,
                                                               @RequestParam String slot) {
        return GlobalResponse.ok(routineService.getRoutineDay(routineId, SecurityUtil.getUserId(), parseSlot(slot)));
    }

    // 사용자가 시간대별로 선택한 KEEP/REMOVE 값을 저장하고 루틴의 최종 제품 수를 갱신한다.
    @PatchMapping("/{routineId}/products")
    public GlobalResponse<UUID> saveRoutine(@PathVariable UUID routineId,
                                            @RequestBody RoutineSaveRequestDto request) {
        return GlobalResponse.ok(routineService.saveRoutine(routineId, SecurityUtil.getUserId(), request));
    }

    // "morning"/"MORNING", "evening"/"EVENING" 등 대소문자 상관없이 파싱
    private RoutineSlot parseSlot(String slot) {
        try {
            return RoutineSlot.valueOf(slot.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("잘못된 slot 값입니다(MORNING 또는 EVENING): " + slot);
        }
    }
}
