package com.nyyb.nyybserver.profile.data.entity;

import com.nyyb.nyybserver.user.data.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "profile",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_profile_user", columnNames = {"user_id"})
        }
)
@EntityListeners(AuditingEntityListener.class)
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ── 현재 루틴 점수 ──
    @Column
    private Integer score; // 100분위 점수 (예: 66)

    @Column(columnDefinition = "TEXT")
    private String scoreReason;

    @Column(name = "latest_analysis_id")
    private UUID latestAnalysisId; // 점수 산출 기준 분석

    // ── 마이탭 통계 ──
    @Column(nullable = false)
    @Builder.Default
    private Integer keptCount = 0; // 쓰는 제품 (최신 루틴 기준)

    @Column(nullable = false)
    @Builder.Default
    private Integer removedCount = 0; // 덜어낸 제품 (최신 루틴 기준)

    @Column(nullable = false)
    @Builder.Default
    private Integer analysisCount = 0; // 분석 횟수 (누적)

    @LastModifiedDate
    @Column
    private LocalDateTime updatedAt;
}