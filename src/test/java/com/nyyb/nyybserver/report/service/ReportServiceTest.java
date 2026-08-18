package com.nyyb.nyybserver.report.service;

import com.nyyb.nyybserver.analysis.data.entity.Product;
import com.nyyb.nyybserver.analysis.data.entity.ProductIngredient;
import com.nyyb.nyybserver.analysis.data.enums.ProductCategory;
import com.nyyb.nyybserver.analysis.data.enums.RecommendStatus;
import com.nyyb.nyybserver.analysis.data.enums.RoutineSlot;
import com.nyyb.nyybserver.analysis.data.repository.ProductIngredientRepository;
import com.nyyb.nyybserver.ingredient.data.entity.Ingredient;
import com.nyyb.nyybserver.ingredient.data.enums.RiskLevel;
import com.nyyb.nyybserver.report.data.dto.response.ReportResponseDto;
import com.nyyb.nyybserver.report.data.enums.ReportStatus;
import com.nyyb.nyybserver.routine.data.entity.Routine;
import com.nyyb.nyybserver.routine.data.entity.RoutineItem;
import com.nyyb.nyybserver.routine.data.entity.RoutineItemSelection;
import com.nyyb.nyybserver.routine.data.exception.RoutineNotFoundException;
import com.nyyb.nyybserver.routine.data.repository.RoutineItemRepository;
import com.nyyb.nyybserver.routine.data.repository.RoutineRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private RoutineRepository routineRepository;

    @Mock
    private RoutineItemRepository routineItemRepository;

    @Mock
    private ProductIngredientRepository productIngredientRepository;

    @InjectMocks
    private ReportService reportService;

    @Test
    void getReportBuildsSlotSpecificRecommendationsAndIngredients() {
        UUID reportId = UUID.randomUUID();
        Long userId = 7L;
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 17, 15, 0);

        Routine routine = Routine.builder()
                .id(reportId)
                .title("8월 17일 루틴 리포트")
                .score(82)
                .scoreReason("중복 성분을 줄인 루틴입니다.")
                .summary("저녁에는 이 제품을 제외해 보세요.")
                .createdAt(createdAt)
                .build();

        Product product = Product.builder()
                .id(11L)
                .imageKey("analysis/test.png")
                .category(ProductCategory.SERUM)
                .productName("테스트 세럼")
                .build();

        RoutineItemSelection morningSelection = RoutineItemSelection.builder()
                .slot(RoutineSlot.MORNING)
                .action(RecommendStatus.KEEP)
                .build();
        RoutineItemSelection eveningSelection = RoutineItemSelection.builder()
                .slot(RoutineSlot.EVENING)
                .action(RecommendStatus.REMOVE)
                .build();

        RoutineItem item = RoutineItem.builder()
                .routine(routine)
                .product(product)
                .userRoutineSlot(RoutineSlot.BOTH)
                .llmRoutineSlot(RoutineSlot.EVENING)
                .recommended(RecommendStatus.REMOVE)
                .recommendReason("저녁 루틴의 다른 제품과 성분이 중복됩니다.")
                .selections(List.of(morningSelection, eveningSelection))
                .build();

        Ingredient ingredient = Ingredient.builder()
                .id(3L)
                .name("나이아신아마이드")
                .riskLevel(RiskLevel.LOW)
                .isToxic(false)
                .description("피부 컨디셔닝 성분")
                .build();

        List<ProductIngredient> ingredients = List.of(
                ProductIngredient.builder()
                        .product(product)
                        .ingredient(ingredient)
                        .rawName("나이아신아마이드")
                        .build(),
                ProductIngredient.builder()
                        .product(product)
                        .rawName("미확인성분")
                        .build()
        );

        when(routineRepository.findByIdAndUserId(reportId, userId))
                .thenReturn(Optional.of(routine));
        when(routineItemRepository.findByRoutineIdWithProductAndSelections(reportId))
                .thenReturn(List.of(item));
        when(productIngredientRepository.findByProductIdWithIngredient(product.getId()))
                .thenReturn(ingredients);

        ReportResponseDto response = reportService.getReport(reportId, userId);

        assertEquals(reportId, response.reportId());
        assertEquals(ReportStatus.READY, response.status());
        assertEquals(82, response.score());
        assertEquals(createdAt, response.createdAt());
        assertEquals(1, response.productCount().analyzed());
        assertEquals(1, response.productCount().selected());
        assertEquals(0, response.productCount().removed());

        assertEquals(RecommendStatus.KEEP, response.morning().products().getFirst().recommended());
        assertNull(response.morning().products().getFirst().recommendReason());
        assertTrue(response.morning().products().getFirst().selected());

        assertEquals(RecommendStatus.REMOVE, response.evening().products().getFirst().recommended());
        assertFalse(response.evening().products().getFirst().selected());
        assertEquals(2, response.evening().products().getFirst().ingredients().size());
        assertTrue(response.evening().products().getFirst().ingredients().getFirst().matched());
        assertFalse(response.evening().products().getFirst().ingredients().get(1).matched());
    }

    @Test
    void getLatestReportRejectsUserWithoutRoutine() {
        Long userId = 99L;
        when(routineRepository.findFirstByUserIdOrderByCreatedAtDescIdDesc(userId))
                .thenReturn(Optional.empty());

        assertThrows(RoutineNotFoundException.class,
                () -> reportService.getLatestReport(userId));
    }
}
