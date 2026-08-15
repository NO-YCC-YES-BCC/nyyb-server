package com.nyyb.nyybserver.event.controller;

import com.nyyb.nyybserver.common.response.GlobalResponse;
import com.nyyb.nyybserver.event.service.EventService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
@Tag(name = "Event", description = "Validation funnel event collection")
public class EventController {

    private final EventService eventService;

    // 검증 페이지 퍼널 이벤트 수집 (비로그인, sendBeacon으로 호출됨)
    @PostMapping
    public GlobalResponse<Void> collect(@RequestBody Map<String, Object> body) {
        eventService.collect(body);
        return GlobalResponse.ok();
    }
}
