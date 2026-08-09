package com.nyyb.nyybserver.report.data.entity;

import com.nyyb.nyybserver.user.data.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "weekly_report",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_weekly_report_user_week", columnNames = {"user_id", "week_start"})
        }
)
@EntityListeners(AuditingEntityListener.class)
public class WeeklyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart; // 월요일 날짜 (캐시 키)

    @Column(columnDefinition = "TEXT")
    private String content; // JSON: { summary, dailyUsage[], recommendedRange[] }

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
