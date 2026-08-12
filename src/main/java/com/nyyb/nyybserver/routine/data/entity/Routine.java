package com.nyyb.nyybserver.routine.data.entity;

import com.nyyb.nyybserver.analysis.data.entity.Analysis;
import com.nyyb.nyybserver.user.data.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "routine")
@EntityListeners(AuditingEntityListener.class)
public class Routine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id; // = routineId

    // nullable: 게스트는 null, claim 시 채움
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // 분석 1개 : 루틴 0..1 (단일 플로우) — 한 분석당 루틴 최대 1개 보장
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", unique = true)
    private Analysis analysis;

    @Column
    private Integer beforeCount; // 분석 전 제품 개수

    @Column
    private Integer afterCount; // 유저가 유지 선택한 개수

    @Column(columnDefinition = "TEXT")
    private String expectedChange; // JSON: LLM 예상변화

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}