package com.nyyb.nyybserver.analysis.data.dto.request;

import com.nyyb.nyybserver.analysis.data.enums.RoutineSlot;
import lombok.Data;

import java.util.List;

@Data
public class AnalysisRequestDto {

    // productId별 유저가 고른 슬롯(아침/저녁/전체)
    private List<ProductSlot> products;

    @Data
    public static class ProductSlot {
        private Long productId;
        private RoutineSlot userRoutineSlot;
    }
}
