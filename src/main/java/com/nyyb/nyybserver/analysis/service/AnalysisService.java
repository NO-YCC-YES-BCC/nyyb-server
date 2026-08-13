package com.nyyb.nyybserver.analysis.service;

import com.nyyb.nyybserver.analysis.data.dto.request.AnalysisRequestDto;
import com.nyyb.nyybserver.analysis.data.dto.response.AnalysisResponseDto;
import com.nyyb.nyybserver.analysis.data.dto.response.LlmAnalysisResponseDto;
import com.nyyb.nyybserver.analysis.data.dto.response.LlmProductAnalysisDto;
import com.nyyb.nyybserver.analysis.data.entity.Analysis;
import com.nyyb.nyybserver.analysis.data.entity.Product;
import com.nyyb.nyybserver.analysis.data.entity.ProductIngredient;
import com.nyyb.nyybserver.analysis.data.enums.RecommendStatus;
import com.nyyb.nyybserver.analysis.data.repository.AnalysisRepository;
import com.nyyb.nyybserver.analysis.data.repository.ProductIngredientRepository;
import com.nyyb.nyybserver.analysis.data.repository.ProductRepository;
import com.nyyb.nyybserver.ingredient.data.entity.Ingredient;
import com.nyyb.nyybserver.routine.data.entity.Routine;
import com.nyyb.nyybserver.routine.data.entity.RoutineItem;
import com.nyyb.nyybserver.routine.data.repository.RoutineItemRepository;
import com.nyyb.nyybserver.routine.data.repository.RoutineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final ChatClient chatClient;
    private final AnalysisRepository analysisRepository;
    private final ProductRepository productRepository;
    private final ProductIngredientRepository productIngredientRepository;
    private final RoutineRepository routineRepository;
    private final RoutineItemRepository routineItemRepository;

    /**
     * 제품들 -> LLM 제외/유지 분석 -> Product·Analysis 반영 -> Routine·RoutineItem 생성(saveRoutine 통합) -> routineId + LLM 응답 반환
     * @param request productId + userRoutineSlot 목록
     * @return AnalysisResponseDto (routineId + 제품별 분석 결과)
     */
    @Transactional
    public AnalysisResponseDto analyze(AnalysisRequestDto request) {
        List<AnalysisRequestDto.ProductSlot> productSlots = request.getProducts();
        List<Long> productIds = productSlots.stream()
                .map(AnalysisRequestDto.ProductSlot::getProductId)
                .toList();

        String userMessage = buildUserMessage(productIds);
        log.info("OpenAI 요청 메시지:\n{}", userMessage);

        // LLM 호출 + 구조화 출력(JSON -> DTO)
        LlmAnalysisResponseDto llmResponse = chatClient.prompt()
                .user(userMessage)
                .call()
                .entity(LlmAnalysisResponseDto.class);

        log.info("OpenAI 응답:\n{}", llmResponse);

        // Analysis 저장 후 결과를 각 Product에 반영(더티 체킹)
        Analysis analysis = analysisRepository.save(Analysis.builder().build());
        llmResponse.products().forEach(result -> applyToProduct(analysis, result));

        // saveRoutine 통합: 분석 1개당 Routine 1개 생성 + productId별 userRoutineSlot을 RoutineItem으로 저장
        Routine routine = routineRepository.save(Routine.builder()
                .analysis(analysis)
                .beforeCount(productIds.size())
                .build());

        for (AnalysisRequestDto.ProductSlot productSlot : productSlots) {
            Product product = productRepository.findById(productSlot.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 productId: " + productSlot.getProductId()));
            routineItemRepository.save(RoutineItem.builder()
                    .routine(routine)
                    .product(product)
                    .userRoutineSlot(productSlot.getUserRoutineSlot())
                    .build());
        }

        // REMOVE 먼저, KEEP 나중 순으로 정렬해 반환
        List<LlmProductAnalysisDto> sorted = llmResponse.products().stream()
                .sorted(Comparator.comparingInt(p -> p.recommended() == RecommendStatus.REMOVE ? 0 : 1))
                .toList();
        return new AnalysisResponseDto(routine.getId(), sorted);
    }

    // 제품별 productId + category + ocrText + 성분 -> 프롬프트 텍스트로 조립
    private String buildUserMessage(List<Long> productIds) {
        StringBuilder sb = new StringBuilder();
        sb.append("다음 제품들을 분석해 주세요.\n\n");

        for (Long productId : productIds) {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 productId: " + productId));

            sb.append("=== productId: ").append(productId).append(" ===\n");
            sb.append("category: ").append(product.getCategory().name()).append("\n");
            sb.append("ocrText: ").append(product.getOcrText()).append("\n");
            sb.append("ingredients: ").append(formatIngredients(productId)).append("\n\n");
        }

        return sb.toString();
    }

    // 해당 제품의 성분을 "성분명(위험도) - 설명" 형태로 나열
    private String formatIngredients(Long productId) {
        List<ProductIngredient> productIngredients =
                productIngredientRepository.findByProductIdWithIngredient(productId);

        if (productIngredients.isEmpty()) {
            return "(인식된 성분 없음)";
        }

        return productIngredients.stream()
                .map(this::formatIngredient)
                .reduce((a, b) -> a + "\n  - " + b)
                .map(s -> "\n  - " + s)
                .orElse("(인식된 성분 없음)");
    }

    private String formatIngredient(ProductIngredient pi) {
        Ingredient ingredient = pi.getIngredient();
        // 마스터 매칭 실패 시 ingredient == null → OCR 원문(rawName) 사용
        if (ingredient == null) {
            return pi.getRawName();
        }
        String risk = ingredient.getRiskLevel() != null ? ingredient.getRiskLevel().name() : "UNKNOWN";
        String toxic = Boolean.TRUE.equals(ingredient.getIsToxic()) ? ", 독성" : "";
        String description = ingredient.getDescription() != null ? " - " + ingredient.getDescription() : "";
        return ingredient.getName() + "(" + risk + toxic + ")" + description;
    }

    // 결과 -> 같은 productId Product에 반영
    private void applyToProduct(Analysis analysis, LlmProductAnalysisDto result) {
        Product product = productRepository.findById(result.productId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 productId: " + result.productId()));

        product.applyAnalysis(analysis, result.productName(), result.recommended(), result.recommendReason());
    }
}
