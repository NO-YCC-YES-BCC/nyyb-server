package com.nyyb.nyybserver.analysis.data.enums;

// 유저 선택: 시간대(오전/오후/전체) + 유지(KEPT)/제거(REMOVED) 6종 결합
public enum RoutineItemStatus {
    MORNING_KEPT,     // 오전 유지
    MORNING_REMOVED,  // 오전 제거
    EVENING_KEPT,     // 오후 유지
    EVENING_REMOVED,  // 오후 제거
    BOTH_KEPT,        // 전체 유지
    BOTH_REMOVED;     // 전체 제거

    // 제거 여부
    public boolean isRemoved() {
        return this == MORNING_REMOVED || this == EVENING_REMOVED || this == BOTH_REMOVED;
    }

    // 제거 시 차감 개수: 오전/오후 = 1, 전체(BOTH) = 2, 유지 = 0
    public int removedCount() {
        if (!isRemoved()) {
            return 0;
        }
        return this == BOTH_REMOVED ? 2 : 1;
    }
}
