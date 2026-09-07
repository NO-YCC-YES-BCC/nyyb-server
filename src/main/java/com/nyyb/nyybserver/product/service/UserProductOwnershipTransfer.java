package com.nyyb.nyybserver.product.service;

import com.nyyb.nyybserver.product.data.repository.UserProductRepository;
import com.nyyb.nyybserver.user.data.repository.UserRepository;
import com.nyyb.nyybserver.user.service.GuestDataOwnershipTransfer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 게스트→소셜 병합 시 게스트가 담은 UserProduct 소유권을 소셜 유저로 이전한다.
 * 이전하지 않으면 병합 후 게스트 시절 분석에 담긴 제품이 주인 없이 남는다.
 * (제품 마스터 Product는 공용 카탈로그라 소유자가 없고 이전 대상도 아니다)
 * KakaoService가 병합 트랜잭션 안에서 호출한다.
 */
@Component
@RequiredArgsConstructor
public class UserProductOwnershipTransfer implements GuestDataOwnershipTransfer {

    private final UserProductRepository userProductRepository;
    private final UserRepository userRepository;

    @Override
    public void transfer(Long guestUserId, Long targetUserId) {
        userProductRepository.transferOwner(
                userRepository.getReferenceById(guestUserId),
                userRepository.getReferenceById(targetUserId));
    }
}
