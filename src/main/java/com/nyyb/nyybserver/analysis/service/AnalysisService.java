package com.nyyb.nyybserver.analysis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nyyb.nyybserver.analysis.data.dto.response.IngredientSummaryDto;
import com.nyyb.nyybserver.analysis.data.dto.response.OcrApiResponseDto;
import com.nyyb.nyybserver.analysis.data.dto.response.OcrResponseDto;
import com.nyyb.nyybserver.analysis.data.entity.Product;
import com.nyyb.nyybserver.analysis.data.entity.ProductIngredient;
import com.nyyb.nyybserver.analysis.data.enums.ProductCategory;
import com.nyyb.nyybserver.analysis.data.exception.InvalidImageException;
import com.nyyb.nyybserver.analysis.data.exception.OcrApiException;
import com.nyyb.nyybserver.analysis.data.exception.UnsupportedImageFormatException;
import com.nyyb.nyybserver.analysis.data.repository.ProductIngredientRepository;
import com.nyyb.nyybserver.analysis.data.repository.ProductRepository;
import com.nyyb.nyybserver.common.s3.S3Service;
import com.nyyb.nyybserver.ingredient.data.entity.Ingredient;
import com.nyyb.nyybserver.ingredient.service.IngredientIndex;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private static final Set<String> SUPPORTED_FORMATS = Set.of(
            "jpg", "jpeg", "png", "pdf", "tiff"
    );


    private final ProductRepository productRepository;
    private final ProductIngredientRepository productIngredientRepository;
    private final RestClient clovaOcrRestClient;
    private final ObjectMapper objectMapper;
    private final S3Service s3Service;
    private final IngredientIndex ingredientIndex;

    @Value("${clova-ocr.secret-key}")
    private String secretKey;


    /**
     * CLOVA General OCR - multipart/form-data
     * message, file
     */
    public OcrResponseDto requestOcr(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new InvalidImageException();
        }

        MultiValueMap<String, Object> body = buildMultipartBody(image);
        OcrApiResponseDto result;

        try {
            result = clovaOcrRestClient.post()
                    .header("X-OCR-SECRET", secretKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(OcrApiResponseDto.class);
        } catch (RestClientException e) {
            log.error("CLOVA OCR API 호출 중 오류가 발생했습니다.", e);
            throw new OcrApiException();
        }

        // 이미지 S3 업로드 (key만 DB 저장, URL은 조회 시점에 발급)
        String imageKey = s3Service.upload(image, "analysis");
        String imageUrl = s3Service.getPresignedUrl(imageKey);



        // 1. OCR 필드 join (lineBreak=true면 개행, 아니면 공백으로 이어붙임)
        String ocrText = buildOcrText(result);

        // 2. 제품 카테고리 분류
        ProductCategory productCategory = ProductCategory.classify(ocrText);

        // 제품 저장
        Product product = Product.builder()
                .imageKey(imageKey)
                .category(productCategory)
                .ocrText(ocrText)
                .build();
        productRepository.save(product);

        // 3. 성분 매칭 (OCR 텍스트 -> 성분 인덱스 조회 -> ProductIngredient 저장)
        List<IngredientSummaryDto> ingredients = matchIngredients(ocrText, product);

        return OcrResponseDto.builder()
                .productId(product.getId())
                .imageUrl(imageUrl)
                .category(productCategory)
                .ingredients(ingredients)
                .build();
    }

    // ocrText db 성분 매칭
    private List<IngredientSummaryDto> matchIngredients(String ocrText, Product product) {
        if (!StringUtils.hasText(ocrText)) {
            return List.of();
        }

        // 성분 id 기준 중복 제거 + 입력 순서 유지
        Map<Long, ProductIngredient> matched = new LinkedHashMap<>();

        for (String token : ocrText.split(",\\s+|\\n")) {
            // 앞뒤 공백 및 꼬리 특수문자(구분자 오독 등) 제거
            String rawName = token.strip().replaceAll("[\\s|]+$", "");
            if (rawName.isEmpty()) {
                continue;
            }

            Ingredient ingredient = ingredientIndex.match(rawName);
            if (ingredient == null) {
                continue; // 제목/노이즈 등 매칭 실패 토큰은 저장하지 않음
            }

            matched.putIfAbsent(ingredient.getId(), ProductIngredient.builder()
                    .product(product)
                    .ingredient(ingredient)
                    .rawName(rawName)
                    .build());
        }

        productIngredientRepository.saveAll(matched.values());

        return matched.values().stream()
                .map(pi -> toSummary(pi.getIngredient()))
                .toList();
    }

    private IngredientSummaryDto toSummary(Ingredient ingredient) {
        return IngredientSummaryDto.builder()
                .ingredientId(ingredient.getId())
                .name(ingredient.getName())
                .isToxic(ingredient.getIsToxic())
                .riskLevel(ingredient.getRiskLevel())
                .build();
    }


    // OCR 결과 -> text로 전환
    private String buildOcrText(OcrApiResponseDto result) {
        StringBuilder sb = new StringBuilder();
        result.images().stream()
                .flatMap(img -> img.fields().stream())
                .forEach(field -> sb.append(field.inferText())
                        .append(field.lineBreak() ? "\n" : " "));
        return sb.toString().strip();
    }

    // message + file 를 하나로
    private MultiValueMap<String, Object> buildMultipartBody(MultipartFile image) {
        HttpHeaders messageHeaders = new HttpHeaders();
        messageHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> messagePart = new HttpEntity<>(buildMessage(image), messageHeaders);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("message", messagePart);
        body.add("file", toResource(image));
        return body;
    }

    // message - 메타데이터 JSON
    private String buildMessage(MultipartFile image) {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("version", "V2");
        message.put("requestId", UUID.randomUUID().toString());
        message.put("timestamp", Instant.now().toEpochMilli());

        ArrayNode images = message.putArray("images");
        ObjectNode imageNode = images.addObject();
        imageNode.put("format", resolveFormat(image));
        imageNode.put("name", "image");

        return message.toString();
    }

    //파일 확장자(jpg/png) -> format에
    private String resolveFormat(MultipartFile image) {
        String filename = image.getOriginalFilename();
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            log.warn("파일 확장자를 확인할 수 없습니다. filename={}", filename);
            throw new UnsupportedImageFormatException();
        }

        String format = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        if (!SUPPORTED_FORMATS.contains(format)) {
            log.warn("지원하지 않는 이미지 형식입니다. format={}", format);
            throw new UnsupportedImageFormatException();
        }
        return format;
    }

    //MultipartFile -> file 보낼 수 있게 변환
    private Resource toResource(MultipartFile image) {
        try {
            return new ByteArrayResource(image.getBytes()) {
                @Override
                public String getFilename() {
                    return StringUtils.hasText(image.getOriginalFilename())
                            ? image.getOriginalFilename()
                            : "image";
                }
            };
        } catch (IOException e) {
            log.warn("이미지 파일을 읽을 수 없습니다.", e);
            throw new InvalidImageException();
        }
    }
}
