package com.nyyb.nyybserver.file.service;

import com.nyyb.nyybserver.file.data.exception.ImageUploadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    /**
     * 이미지를 S3에 업로드하고 저장된 key를 반환한다. (key만 DB에 저장)
     */
    public String upload(MultipartFile image, String dirName) {
        String key = dirName + "/" + UUID.randomUUID() + "-" + image.getOriginalFilename();
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(image.getContentType())
                    .contentLength(image.getSize())
                    .build();
            s3Client.putObject(request, RequestBody.fromInputStream(image.getInputStream(), image.getSize()));
        } catch (IOException | S3Exception e) {
            log.error("이미지 S3 업로드 중 오류가 발생했습니다. key={}", key, e);
            throw new ImageUploadException();
        }
        return key;
    }
}
