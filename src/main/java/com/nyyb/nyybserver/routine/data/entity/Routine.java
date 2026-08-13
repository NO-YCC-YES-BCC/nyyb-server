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

    // 분석 1 : 루틴 1 (유니크 제약은 걸지 않음)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id")
    private Analysis analysis;

    @Column
    private Integer score; // 100분위 점수 (예: 66)

    @Column(columnDefinition = "TEXT")
    private String scoreReason;

    @Column
    private String summary;

    @Column
    private Integer beforeCount; // 분석 전 제품 개수

    @Column
    private Integer afterCount; // 유저가 제거 선택한 개수

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // createRoutine 단계에서 LLM 루틴 설계 결과를 반영
    public void applyLlmRoutine(Integer score, String scoreReason, String summary) {
        this.score = score;
        this.scoreReason = scoreReason;
        this.summary = summary;
    }

    // saveRoutine 단계에서 유저가 제거 선택한 개수를 반영
    public void applyAfterCount(Integer afterCount) {
        this.afterCount = afterCount;
    }
}