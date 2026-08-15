package com.nyyb.nyybserver.analysis.service;

import com.nyyb.nyybserver.analysis.data.repository.ProductRepository;
import com.nyyb.nyybserver.user.data.repository.UserRepository;
import com.nyyb.nyybserver.user.service.GuestDataOwnershipTransfer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 게스트→소셜 병합 시 게스트가 OCR로 만든 Product 소유권을 소셜 유저로 이전한다.
 * 이전하지 않으면 병합 후 게스트 시절 업로드한 제품으로 분석을 만들 수 없다.
 * AuthService가 병합 트랜잭션 안에서 호출한다.
 */
@Component
@RequiredArgsConstructor
public class ProductOwnershipTransfer implements GuestDataOwnershipTransfer {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    public void transfer(Long guestUserId, Long targetUserId) {
        productRepository.transferOwner(
                userRepository.getReferenceById(guestUserId),
                userRepository.getReferenceById(targetUserId));
    }
}
