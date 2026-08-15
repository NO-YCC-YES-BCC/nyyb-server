package com.nyyb.nyybserver.routine.data.entity;

import com.nyyb.nyybserver.analysis.data.enums.RecommendStatus;
import com.nyyb.nyybserver.analysis.data.enums.RoutineSlot;
import jakarta.persistence.*;
import lombok.*;

/**
 * 유저의 슬롯별 선택 레코드.
 * (routineItem, slot) 단위로 하나씩 존재하며 action(KEEP/REMOVE)만 담는다.
 * - MORNING 전용 제품: MORNING 레코드 1개
 * - BOTH 제품: MORNING·EVENING 레코드 각 1개 (양쪽 제거 시 REMOVE 레코드 2개)
 * slot에는 MORNING/EVENING만 저장한다(BOTH는 두 슬롯으로 나눠 저장).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "routine_item_selection",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_routine_item_slot",
                columnNames = {"routine_item_id", "slot"}
        )
)
public class RoutineItemSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 루틴아이템 1 : 슬롯선택 0..2 (MORNING/EVENING)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_item_id", nullable = false)
    private RoutineItem routineItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoutineSlot slot; // MORNING 또는 EVENING (BOTH 저장하지 않음)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecommendStatus action; // 유저 선택 (KEEP / REMOVE)

    void changeAction(RecommendStatus action) {
        this.action = action;
    }
}
