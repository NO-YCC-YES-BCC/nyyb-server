package com.nyyb.nyybserver.analysis.service;

import com.nyyb.nyybserver.analysis.data.dto.response.CompatibilityResponseDto;
import com.nyyb.nyybserver.analysis.data.dto.response.LlmCompatibilityIssueDto;
import com.nyyb.nyybserver.analysis.data.dto.response.LlmCompatibilityResponseDto;
import com.nyyb.nyybserver.analysis.data.dto.response.OcrIngredientDto;
import com.nyyb.nyybserver.analysis.data.dto.response.OcrResponseDto;
import com.nyyb.nyybserver.analysis.data.entity.Product;
import com.nyyb.nyybserver.analysis.data.entity.ProductIngredient;
import com.nyyb.nyybserver.analysis.data.enums.CompatibilityStatus;
import com.nyyb.nyybserver.analysis.data.enums.ProductCategory;
import com.nyyb.nyybserver.analysis.data.enums.RecommendStatus;
import com.nyyb.nyybserver.analysis.data.enums.RoutineSlot;
import com.nyyb.nyybserver.analysis.data.repository.ProductIngredientRepository;
import com.nyyb.nyybserver.analysis.data.repository.ProductRepository;
import com.nyyb.nyybserver.ingredient.data.entity.Allergic;
import com.nyyb.nyybserver.ingredient.data.entity.Ingredient;
import com.nyyb.nyybserver.ingredient.data.enums.RiskLevel;
import com.nyyb.nyybserver.ingredient.data.repository.AllergicRepository;
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
import org.springframework.mock.web.MockMultipartFile;

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
    private OcrService ocrService;

    @Mock
    private CompatibilityAnalyzer compatibilityAnalyzer;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductIngredientRepository productIngredientRepository;

    @Mock
    private AllergicRepository allergicRepository;

    @Mock
    private RoutineRepository routineRepository;

    @Mock
    private RoutineItemRepository routineItemRepository;

    @InjectMocks
    private CompatibilityService compatibilityService;

    @Test
    void compareUsesOnlyProductsKeptInTheCurrentRoutine() {
        Long userId = 7L;
        UUID routineId = UUID.randomUUID();
        MockMultipartFile image = new MockMultipartFile(
                "file", "ingredients.jpg", "image/jpeg", new byte[]{1, 2, 3}
        );

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
        Ingredient prohibitedIngredient = ingredient(
                2L,
                "갈란타민",
                RiskLevel.DISALLOWED,
                "화장품 안전기준 등에 관한 규정 별표 1의 사용금지 원료. 분류: 사용금지."
        );
        Ingredient restrictedIngredient = ingredient(
                3L,
                "제한성분",
                RiskLevel.DISALLOWED,
                "별표 2의 사용조건 확인이 필요한 제한 원료. 분류: 기타. 사용기준: 0.01%."
        );
        Ingredient unverifiedIngredient = Ingredient.builder()
                .id(4L)
                .name("근거 문구가 없는 성분")
                .riskLevel(RiskLevel.HIGH)
                .isToxic(true)
                .description("별도의 규정 문구가 등록되지 않은 설명")
                .build();
        Allergic registeredAllergen = Allergic.builder()
                .id(1L)
                .name(limonene.getName())
                .dataSource("식품의약품안전처 고시")
                .build();
        ProductIngredient candidateIngredient = productIngredient(candidate, limonene);
        ProductIngredient prohibitedProductIngredient = productIngredient(candidate, prohibitedIngredient);
        ProductIngredient restrictedProductIngredient = productIngredient(candidate, restrictedIngredient);
        ProductIngredient unverifiedProductIngredient = productIngredient(candidate, unverifiedIngredient);
        ProductIngredient keptIngredient = productIngredient(keptProduct, limonene);

        OcrIngredientDto ocrIngredient = OcrIngredientDto.builder()
                .ingredientId(limonene.getId())
                .name(limonene.getName())
                .isToxic(false)
                .riskLevel(RiskLevel.LOW)
                .build();
        OcrResponseDto ocrResult = OcrResponseDto.builder()
                .productId(candidate.getId())
                .category(candidate.getCategory())
                .ingredientCount(1)
                .ingredients(List.of(ocrIngredient))
                .build();

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

        when(routineRepository.findFirstByUserIdOrderByCreatedAtDescIdDesc(userId))
                .thenReturn(Optional.of(routine));
        when(ocrService.ocr(image, userId)).thenReturn(ocrResult);
        when(productRepository.findByIdAndUserId(candidate.getId(), userId))
                .thenReturn(Optional.of(candidate));
        when(productIngredientRepository.findByProductIdWithIngredient(candidate.getId()))
                .thenReturn(List.of(
                        candidateIngredient,
                        prohibitedProductIngredient,
                        restrictedProductIngredient,
                        unverifiedProductIngredient
                ));
        when(routineItemRepository.findByRoutineIdWithProductAndSelections(routineId))
                .thenReturn(List.of(keptItem, removedItem));
        when(productIngredientRepository.findByProductIdWithIngredient(keptProduct.getId()))
                .thenReturn(List.of(keptIngredient));
        when(allergicRepository.findAll()).thenReturn(List.of(registeredAllergen));
        when(compatibilityAnalyzer.analyze(anyString())).thenReturn(llmResponse);

        CompatibilityResponseDto response = compatibilityService.compare(image, userId);

        assertEquals(candidate.getId(), response.productId());
        assertEquals("테스트 세럼", response.productName());
        assertEquals(CompatibilityStatus.CAUTION, response.status());
        assertEquals(100, response.score());
        assertEquals(RoutineSlot.EVENING, response.recommendedSlot());
        assertEquals(
                "현재 루틴과 겹치는 구성이 있어 사용 시간대를 나누는 방법을 고려해볼 수 있어요.",
                response.summary()
        );
        assertEquals(
                "겹치는 제품과 서로 다른 시간대에 사용하는 방법을 고려해볼 수 있어요.",
                response.usageGuide()
        );
        assertEquals(
                "본 분석은 성분 정보 참고용이며, 효능·안전성을 진단·보증하지 않습니다.",
                response.disclaimer()
        );
        assertEquals(1, response.issues().size());
        assertEquals(keptProduct.getId(), response.issues().getFirst().routineProductId());
        assertEquals("기존 세럼", response.issues().getFirst().routineProductName());
        assertEquals(1, response.issues().getFirst().overlappingIngredientCount());
        assertEquals(List.of("리모넨"), response.issues().getFirst().overlappingIngredients());
        assertEquals(
                "성분 또는 제품 역할 구성이 겹쳐 사용 시간대를 나누는 방법을 고려해볼 수 있어요.",
                response.issues().getFirst().reason()
        );
        assertEquals("분석이 모두 완료되었어요!", response.reportTitle());
        assertEquals("구매 예정인 제품과 기존 제품의 성분을 비교했어요.", response.reportDescription());
        assertEquals("구매 전 중복 성분을 한 번 더 확인해볼 수 있어요.", response.purchaseAdvice());
        assertEquals(1, response.duplicateReport().productCount());
        assertEquals(keptProduct.getId(), response.duplicateReport().products().getFirst().routineProductId());
        assertEquals("주의 정보가 등록된 성분이 포함되어 있어요.", response.ingredientNoticeTitle());
        assertEquals(3, response.ingredientNotices().size());
        assertTrue(response.ingredientNotices().getFirst().registeredAllergen());
        assertEquals("식품의약품안전처", response.ingredientNotices().getFirst().source());
        assertEquals(
                "해당 성분은 알레르기 유발 가능성이 있습니다.",
                response.ingredientNotices().getFirst().allergenNotice()
        );
        assertEquals(
                "※ 다만, 사용 후 씻어내는 제품에는 0.01% 초과, 사용 후 씻어내지 않는 제품에는 0.001% 초과 함유하는 경우에 한한다.",
                response.ingredientNotices().getFirst().allergenThresholdNotice()
        );
        assertEquals(
                "사용금지된 성분입니다.",
                response.ingredientNotices().get(1).regulatoryNotice()
        );
        assertEquals(
                "화장품 안전기준 등에 관한 규정 별표 1의 사용금지 원료. 분류: 사용금지.",
                response.ingredientNotices().get(1).description()
        );
        assertEquals(
                "사용기준 0.01%로 제한된 성분입니다.",
                response.ingredientNotices().get(2).regulatoryNotice()
        );
        assertEquals(
                "본 정보는 식품의약품안전처 공개 기준을 그대로 안내하는 것이며, 개별 사용자의 사용 적합성·안전성 또는 알레르기 발생 여부를 진단·보증하는 것이 아닙니다.",
                response.ingredientNotices().getFirst().informationDisclaimer()
        );
        assertFalse(response.ingredientNotices().stream()
                .anyMatch(notice -> notice.name().equals(unverifiedIngredient.getName())));

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(compatibilityAnalyzer).analyze(promptCaptor.capture());
        assertTrue(promptCaptor.getValue().contains("productId: " + keptProduct.getId()));
        assertFalse(promptCaptor.getValue().contains("productId: " + removedProduct.getId()));
    }

    @Test
    void compareChecksForCurrentRoutineBeforeCallingOcr() {
        Long userId = 99L;
        MockMultipartFile image = new MockMultipartFile(
                "file", "ingredients.jpg", "image/jpeg", new byte[]{1}
        );
        when(routineRepository.findFirstByUserIdOrderByCreatedAtDescIdDesc(userId))
                .thenReturn(Optional.empty());

        assertThrows(RoutineNotFoundException.class,
                () -> compatibilityService.compare(image, userId));

        verify(ocrService, never()).ocr(image, userId);
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
        return ingredient(id, name, RiskLevel.LOW, null);
    }

    private Ingredient ingredient(Long id, String name, RiskLevel riskLevel, String description) {
        return Ingredient.builder()
                .id(id)
                .name(name)
                .riskLevel(riskLevel)
                .isToxic(false)
                .description(description)
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
