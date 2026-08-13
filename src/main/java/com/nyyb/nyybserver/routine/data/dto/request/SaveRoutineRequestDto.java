package com.nyyb.nyybserver.routine.data.dto.request;

import com.nyyb.nyybserver.analysis.data.enums.RoutineItemStatus;
import lombok.Data;

import java.util.List;

@Data
public class SaveRoutineRequestDto {

    // 제품별 유저 선택 (KEPT/REMOVED)
    private List<ProductStatus> products;

    @Data
    public static class ProductStatus {
        private Long id;                  // productId
        private RoutineItemStatus status; // KEPT / REMOVED
    }
}
