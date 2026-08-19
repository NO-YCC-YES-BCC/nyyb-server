package com.nyyb.nyybserver.analysis.service;

import com.nyyb.nyybserver.analysis.data.dto.request.CompatibilityRequestDto;
import com.nyyb.nyybserver.analysis.data.dto.response.CompatibilityResponseDto;
import com.nyyb.nyybserver.analysis.data.dto.response.LlmCompatibilityIssueDto;
import com.nyyb.nyybserver.analysis.data.dto.response.LlmCompatibilityResponseDto;
import com.nyyb.nyybserver.analysis.data.entity.Product;
import com.nyyb.nyybserver.analysis.data.entity.ProductIngredient;
import com.nyyb.nyybserver.analysis.data.enums.CompatibilityStatus;
import com.nyyb.nyybserver.analysis.data.enums.ProductCategory;
import com.nyyb.nyybserver.analysis.data.enums.RecommendStatus;
import com.nyyb.nyybserver.analysis.data.enums.RoutineSlot;
import com.nyyb.nyybserver.analysis.data.repository.ProductIngredientRepository;
import com.nyyb.nyybserver.analysis.data.repository.ProductRepository;
import com.nyyb.nyybserver.ingredient.data.dto.response.AllergicDto;
import com.nyyb.nyybserver.ingredient.data.dto.response.IngredientDto;
import com.nyyb.nyybserver.ingredient.data.dto.response.ProductIngredientMatchDto;
import com.nyyb.nyybserver.ingredient.data.entity.Ingredient;
import com.nyyb.nyybserver.ingredient.data.enums.RiskLevel;
import com.nyyb.nyybserver.ingredient.service.IngredientService;
import com.nyyb.nyybserver.routine.data.entity.Routine;
import com.nyyb.nyybserver.routine.data.entity.RoutineItem;
import com.nyyb.nyybserver.routine.data.entity.RoutineItemSelection;
import com.nyyb.nyybserver.routine.data.exception.RoutineNotFoundException;
import com.nyyb.nyybserver.routine.data.repository.RoutineItemRepository;
import com.nyyb.nyybserver.routine.data.repository.RoutineRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompatibilityServiceTest {

    @Mock
    private CompatibilityAnalyzer compatibilityAnalyzer;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductIngredientRepository productIngredientRepository;

    @Mock
    private RoutineRepository routineRepository;

    @Mock
    private RoutineItemRepository routineItemRepository;

    @Mock
    private IngredientService ingredientService;

    @InjectMocks
    private CompatibilityService compatibilityService;

    @Test
    void compareUsesOnlyProductsKeptInTheCurrentRoutine() {
        Long userId = 7L;
        UUID routineId = UUID.randomUUID();

        Routine routine = Routine.builder().id(routineId).title("현재 루틴").build();
        Product candidate = product(200L, "새 제품 성분표", null);
        Product keptProduct = product(101L, "나이아신아마이드, 글리세린", "기존 세럼");
        Product removedProduct = product(102L, "레티놀", "제거한 크림");

        RoutineItem keptItem = routineItem(
                routine,
                keptProduct,
                RecommendStatus.KEEP,
                RoutineItemSelection.builder()
                        .slot(RoutineSlot.MORNING)
                        .action(RecommendStatus.KEEP)
                        .build()
        );
        RoutineItem removedItem = routineItem(
                routine,
                removedProduct,
                RecommendStatus.REMOVE,
                RoutineItemSelection.builder()
                        .slot(RoutineSlot.MORNING)
                        .action(RecommendStatus.REMOVE)
                        .build()
        );

        Ingredient limonene = ingredient(1L, "리모넨");
        ProductIngredient candidateIngredient = productIngredient(candidate, limonene);
        ProductIngredient keptIngredient = productIngredient(keptProduct, limonene);

        LlmCompatibilityResponseDto llmResponse = new LlmCompatibilityResponseDto(
                "테스트 세럼",
                CompatibilityStatus.CAUTION,
                120,
                RoutineSlot.EVENING,
                "이 조합은 피부에 자극을 줘요.",
                "반드시 기존 세럼과 다른 시간대에 사용하세요.",
                List.of(
                        new LlmCompatibilityIssueDto(
                                RoutineSlot.MORNING,
                                keptProduct.getId(),
                                "두 제품을 함께 쓰면 알레르기를 유발해요."
                        ),
                        new LlmCompatibilityIssueDto(
                                RoutineSlot.MORNING,
                                removedProduct.getId(),
                                "입력에 없는 결과는 제거되어야 해요."
                        )
                )
        );

        ProductIngredientMatchDto ingredientMatch = new ProductIngredientMatchDto(
                candidate.getId(),
                "테스트 세럼",
                List.of(new IngredientDto(limonene.getId(), limonene.getName(), false, RiskLevel.LOW, null)),
                List.of(new AllergicDto(1L, limonene.getName(), "식품의약품안전처 고시"))
        );

        when(routineRepository.findByIdAndUserId(routineId, userId))
                .thenReturn(Optional.of(routine));
        when(productRepository.findByIdAndUserId(candidate.getId(), userId))
                .thenReturn(Optional.of(candidate));
        when(productIngredientRepository.findByProductIdWithIngredient(candidate.getId()))
                .thenReturn(List.of(candidateIngredient));
        when(routineItemRepository.findByRoutineIdWithProductAndSelections(routineId))
                .thenReturn(List.of(keptItem, removedItem));
        when(productIngredientRepository.findByProductIdWithIngredient(keptProduct.getId()))
                .thenReturn(List.of(keptIngredient));
        when(ingredientService.match(candidate.getId())).thenReturn(ingredientMatch);
        when(compatibilityAnalyzer.analyze(anyString())).thenReturn(llmResponse);

        CompatibilityRequestDto request = new CompatibilityRequestDto();
        request.setProductId(candidate.getId());
        request.setRoutineId(routineId);

        CompatibilityResponseDto response = compatibilityService.compare(request, userId);

        assertEquals(candidate.getId(), response.productId());
        assertEquals("테스트 세럼", response.productName());
        assertEquals(RecommendStatus.REMOVE, response.recommended());
        assertEquals(
                "제품 " + keptProduct.getId() + "번과 1개 성분 중복\n"
                        + "현재 루틴과 겹치는 구성이 있어 사용 시간대를 나누는 방법을 고려해볼 수 있어요.",
                response.recommendReason()
        );
        assertFalse(response.recommendReason().contains("제품 " + removedProduct.getId() + "번과"));
        assertEquals(ingredientMatch.ingredients(), response.ingredients());
        assertEquals(ingredientMatch.allergics(), response.allergics());

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(compatibilityAnalyzer).analyze(promptCaptor.capture());
        assertTrue(promptCaptor.getValue().contains("productId: " + keptProduct.getId()));
        assertFalse(promptCaptor.getValue().contains("productId: " + removedProduct.getId()));
    }

    @Test
    void compareChecksForCurrentRoutineBeforeLoadingProduct() {
        Long userId = 99L;
        CompatibilityRequestDto request = new CompatibilityRequestDto();
        request.setProductId(200L);
        request.setRoutineId(UUID.randomUUID());

        when(routineRepository.findByIdAndUserId(request.getRoutineId(), userId))
                .thenReturn(Optional.empty());

        assertThrows(RoutineNotFoundException.class,
                () -> compatibilityService.compare(request, userId));

        verify(productRepository, never()).findByIdAndUserId(request.getProductId(), userId);
    }

    private Product product(Long id, String ocrText, String productName) {
        return Product.builder()
                .id(id)
                .imageKey("analysis/" + id + ".jpg")
                .category(ProductCategory.SERUM)
                .productName(productName)
                .ocrText(ocrText)
                .build();
    }

    private RoutineItem routineItem(
            Routine routine,
            Product product,
            RecommendStatus recommended,
            RoutineItemSelection selection
    ) {
        return RoutineItem.builder()
                .routine(routine)
                .product(product)
                .userRoutineSlot(RoutineSlot.MORNING)
                .llmRoutineSlot(RoutineSlot.MORNING)
                .recommended(recommended)
                .selections(List.of(selection))
                .build();
    }

    private Ingredient ingredient(Long id, String name) {
        return Ingredient.builder()
                .id(id)
                .name(name)
                .riskLevel(RiskLevel.LOW)
                .isToxic(false)
                .build();
    }

    private ProductIngredient productIngredient(Product product, Ingredient ingredient) {
        return ProductIngredient.builder()
                .product(product)
                .ingredient(ingredient)
                .rawName(ingredient.getName())
                .build();
    }
}
