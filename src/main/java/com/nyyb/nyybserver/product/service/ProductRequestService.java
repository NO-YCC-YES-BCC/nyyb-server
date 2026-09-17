package com.nyyb.nyybserver.product.service;

import com.nyyb.nyybserver.product.data.dto.request.ProductRequestDto;
import com.nyyb.nyybserver.product.data.entity.ProductRequest;
import com.nyyb.nyybserver.product.data.exception.InvalidProductRequestException;
import com.nyyb.nyybserver.product.data.repository.ProductRequestRepository;
import com.nyyb.nyybserver.user.data.entity.User;
import com.nyyb.nyybserver.user.data.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 제품 추가 등록 요청 저장.
 * 검색 결과가 없을 때 사용자가 남기는 요청이라 받아서 쌓아두기만 한다.
 * 수집 대상에 넣을지는 사람이 목록을 보고 판단한다.
 */
@Service
@RequiredArgsConstructor
public class ProductRequestService {

    private static final int MAX_KEYWORD_LENGTH = 200;

    private final ProductRequestRepository productRequestRepository;
    private final UserRepository userRepository;

    @Transactional
    public void register(ProductRequestDto request, Long userId) {
        String keyword = normalize(request);

        // 요청자를 못 찾아도 요청 자체는 남긴다. 어떤 제품을 찾다 실패했는지가 더 중요하다.
        User user = userId == null ? null : userRepository.findById(userId).orElse(null);

        productRequestRepository.save(ProductRequest.builder()
                .user(user)
                .keyword(keyword)
                .build());
    }

    private String normalize(ProductRequestDto request) {
        if (request == null || request.getKeyword() == null || request.getKeyword().isBlank()) {
            throw new InvalidProductRequestException();
        }
        String keyword = request.getKeyword().strip();
        if (keyword.length() > MAX_KEYWORD_LENGTH) {
            throw new InvalidProductRequestException();
        }
        return keyword;
    }
}
