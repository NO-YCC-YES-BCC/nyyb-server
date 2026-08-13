package com.nyyb.nyybserver.analysis.data.enums;

// 루틴 아이템의 LLM 추천: 시간대(오전/오후/전체) + 유지(KEEP)/제외(REMOVE)를 하나로 결합
public enum RoutineRecommendStatus {
    MORNING_KEEP,    // 오전 유지
    MORNING_REMOVE,  // 오전 제외
    EVENING_KEEP,    // 오후 유지
    EVENING_REMOVE,  // 오후 제외
    BOTH_KEEP,       // 전체 유지
    BOTH_REMOVE;     // 전체 제외

    // recommended 값에서 시간대(RoutineSlot)를 도출
    public RoutineSlot slot() {
        return switch (this) {
            case MORNING_KEEP, MORNING_REMOVE -> RoutineSlot.MORNING;
            case EVENING_KEEP, EVENING_REMOVE -> RoutineSlot.EVENING;
            case BOTH_KEEP, BOTH_REMOVE -> RoutineSlot.BOTH;
        };
    }
}
