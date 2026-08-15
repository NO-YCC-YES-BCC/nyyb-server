package com.nyyb.nyybserver.event.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nyyb.nyybserver.event.data.entity.FunnelEvent;
import com.nyyb.nyybserver.event.data.repository.FunnelEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private static final int MAX_PAYLOAD = 4000;

    private final FunnelEventRepository funnelEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * 프론트가 sendBeacon으로 보낸 퍼널 이벤트를 저장한다.
     * 비로그인 공개 엔드포인트라 값은 전부 신뢰하지 않고 잘라서 넣는다.
     *
     * @param body {event, visitor, ts, utm_*, 그 외 이벤트별 부가 필드}
     */
    @Transactional
    public void collect(Map<String, Object> body) {
        if (body == null) {
            return;
        }
        String eventName = text(body.get("event"), 100);
        if (!StringUtils.hasText(eventName)) {
            return; // 이벤트명 없으면 집계 의미가 없으므로 버린다
        }

        funnelEventRepository.save(FunnelEvent.builder()
                .eventName(eventName)
                .visitorId(text(body.get("visitor"), 64))
                .utmSource(text(body.get("utm_source"), 100))
                .utmMedium(text(body.get("utm_medium"), 100))
                .utmCampaign(text(body.get("utm_campaign"), 150))
                .utmContent(text(body.get("utm_content"), 150))
                .utmTerm(text(body.get("utm_term"), 150))
                .clientTs(epochMillis(body.get("ts")))
                .payload(json(body))
                .build());
    }

    // 문자열로 바꾸고 컬럼 길이에 맞춰 자른다
    private String text(Object value, int max) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value).strip();
        if (s.isEmpty()) {
            return null;
        }
        return s.length() > max ? s.substring(0, max) : s;
    }

    private Long epochMillis(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return value == null ? null : Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String json(Map<String, Object> body) {
        try {
            String s = objectMapper.writeValueAsString(body);
            return s.length() > MAX_PAYLOAD ? s.substring(0, MAX_PAYLOAD) : s;
        } catch (Exception e) {
            log.warn("이벤트 payload 직렬화 실패", e);
            return null;
        }
    }
}
