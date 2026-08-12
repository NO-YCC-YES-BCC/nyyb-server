package com.nyyb.nyybserver.analysis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nyyb.nyybserver.analysis.data.dto.response.OcrApiResponseDto;
import com.nyyb.nyybserver.analysis.data.dto.response.OcrResponseDto;
import com.nyyb.nyybserver.analysis.data.enums.ProductCategory;
import com.nyyb.nyybserver.analysis.data.exception.InvalidImageException;
import com.nyyb.nyybserver.analysis.data.exception.OcrApiException;
import com.nyyb.nyybserver.analysis.data.exception.UnsupportedImageFormatException;
import com.nyyb.nyybserver.file.service.FileService;
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
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private static final Set<String> SUPPORTED_FORMATS = Set.of(
            "jpg", "jpeg", "png", "pdf", "tiff"
    );


    private final RestClient clovaOcrRestClient;
    private final ObjectMapper objectMapper;
    private final FileService fileService;
    private final AnalysisWriter analysisWriter;

    @Value("${clova-ocr.secret-key}")
    private String secretKey;


    /**
     * 사진 -> ocr -> 카테고리 분류, 성분 매칭
     * @param image
     * @return OcrResponseDto
     */
    public OcrResponseDto requestOcr(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new InvalidImageException();
        }

        // 1. 외부 I/O — CLOVA OCR 호출 (multipart/form-data: message, file)
        MultiValueMap<String, Object> body = buildMultipartBody(image);
        OcrApiResponseDto ocrResult;
        try {
            ocrResult = clovaOcrRestClient.post()
                    .header("X-OCR-SECRET", secretKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(OcrApiResponseDto.class);
        } catch (RestClientException e) {
            log.error("CLOVA OCR API 호출 중 오류가 발생했습니다.", e);
            throw new OcrApiException();
        }

        // 2. 외부 I/O — S3 업로드 (key만 DB 저장, URL은 조회 시점에 발급)
        String imageKey = fileService.upload(image, "analysis");

        // 3. 가공 — OCR 필드 join + 카테고리 분류
        String ocrText = buildOcrText(ocrResult);
        ProductCategory category = ProductCategory.classify(ocrText);

        // 4. DB 저장 — 트랜잭션 경계 (별도 빈)
        AnalysisWriter.Result saved = analysisWriter.save(imageKey, category, ocrText);

        // 5. 응답 조립 — 방금 저장한 값 + 즉시 사용할 presigned URL (추가 조회 없음)
        return OcrResponseDto.builder()
                .productId(saved.productId())
                .imageUrl(fileService.getPresignedUrl(imageKey))
                .category(category)
                .ingredients(saved.ingredients())
                .build();
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



    // CLOVA OCR request
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



    // OCR 결과 -> text로 전환
    private String buildOcrText(OcrApiResponseDto result) {
        if (result == null || result.images() == null) {
            log.warn("OCR 응답이 비어 있습니다. result={}", result);
            throw new OcrApiException();
        }

        StringBuilder sb = new StringBuilder();
        result.images().stream()
                .flatMap(img -> img.fields().stream())
                .forEach(field -> sb.append(field.inferText())
                        .append(field.lineBreak() ? "\n" : " "));
        return sb.toString().strip();
    }
}
