package com.nyyb.nyybserver.routine.data.dto.request;

import com.nyyb.nyybserver.analysis.data.enums.RecommendStatus;
import com.nyyb.nyybserver.analysis.data.enums.RoutineSlot;
import lombok.Data;

import java.util.List;

@Data
public class RoutineSaveRequestDto {

    // 제품별 유저 선택 (슬롯 + 유지/제외)
    private List<ProductSelection> products;

    @Data
    public static class ProductSelection {
        private Long id;             // productId
        private RoutineSlot slot;    // MORNING / EVENING (해당 화면의 슬롯)
        private RecommendStatus action; // KEEP / REMOVE
    }
}
