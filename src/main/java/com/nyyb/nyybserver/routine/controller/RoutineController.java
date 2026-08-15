package com.nyyb.nyybserver.routine.controller;

import com.nyyb.nyybserver.analysis.data.enums.RoutineSlot;
import com.nyyb.nyybserver.common.response.GlobalResponse;
import com.nyyb.nyybserver.routine.data.dto.request.SaveRoutineRequestDto;
import com.nyyb.nyybserver.routine.data.dto.response.CreateRoutineResponseDto;
import com.nyyb.nyybserver.routine.data.dto.response.RoutineDayResponseDto;
import com.nyyb.nyybserver.routine.service.RoutineService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequiredArgsConstructor
@RequestMapping("/routines")
@Tag(name = "Routine", description = "Routine design and daily routine APIs")
public class RoutineController {

    private final RoutineService routineService;

    @PostMapping("/{routineId}/design")
    public GlobalResponse<CreateRoutineResponseDto> designRoutine(@PathVariable UUID routineId) {
        return GlobalResponse.ok(routineService.createRoutine(routineId));
    }

    @GetMapping("/{routineId}/day")
    public GlobalResponse<RoutineDayResponseDto> getRoutineDay(@PathVariable UUID routineId,
                                                               @RequestParam String slot) {
        return GlobalResponse.ok(routineService.getRoutineDay(routineId, parseSlot(slot)));
    }

    @PatchMapping("/{routineId}/products")
    public GlobalResponse<UUID> saveRoutine(@PathVariable UUID routineId,
                                            @RequestBody SaveRoutineRequestDto request) {
        return GlobalResponse.ok(routineService.saveRoutine(routineId, request));
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
