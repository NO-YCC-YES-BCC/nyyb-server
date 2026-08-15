package com.nyyb.nyybserver.analysis.service;

import com.nyyb.nyybserver.analysis.data.repository.AnalysisRepository;
import com.nyyb.nyybserver.user.data.repository.UserRepository;
import com.nyyb.nyybserver.user.service.GuestDataOwnershipTransfer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 게스트→소셜 병합 시 게스트가 만든 Analysis 소유권을 소셜 유저로 이전한다.
 * KakaoService가 병합 트랜잭션 안에서 호출한다.
 */
@Component
@RequiredArgsConstructor
public class AnalysisOwnershipTransfer implements GuestDataOwnershipTransfer {

    private final AnalysisRepository analysisRepository;
    private final UserRepository userRepository;

    @Override
    public void transfer(Long guestUserId, Long targetUserId) {
        analysisRepository.transferOwner(
                userRepository.getReferenceById(guestUserId),
                userRepository.getReferenceById(targetUserId));
    }
}
