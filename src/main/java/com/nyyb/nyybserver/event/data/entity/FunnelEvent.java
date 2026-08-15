package com.nyyb.nyybserver.event.data.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 검증 페이지 퍼널 이벤트 1건.
 * 비로그인 방문자가 보내는 값이므로 신뢰하지 않고 길이를 잘라 저장한다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "funnel_event", indexes = {
        @Index(name = "idx_funnel_event_name", columnList = "eventName"),
        @Index(name = "idx_funnel_event_created", columnList = "createdAt"),
        @Index(name = "idx_funnel_event_visitor", columnList = "visitorId")
})
@EntityListeners(AuditingEntityListener.class)
public class FunnelEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // view_landing, start_capture, photo_added, willingness_yes ...
    @Column(nullable = false, length = 100)
    private String eventName;

    // 브라우저 localStorage에 저장된 익명 식별자 (로그인 개념 없음)
    @Column(length = 64)
    private String visitorId;

    @Column(length = 100)
    private String utmSource;

    @Column(length = 100)
    private String utmMedium;

    @Column(length = 150)
    private String utmCampaign;

    @Column(length = 150)
    private String utmContent;

    @Column(length = 150)
    private String utmTerm;

    // 클라이언트가 찍은 시각(epoch millis). 기기 시계라 참고용이며 집계는 createdAt 기준.
    @Column
    private Long clientTs;

    // 이벤트별 부가 필드(recognized, overlaps, reason 등)를 원본 JSON 그대로 보관
    @Column(columnDefinition = "TEXT")
    private String payload;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
