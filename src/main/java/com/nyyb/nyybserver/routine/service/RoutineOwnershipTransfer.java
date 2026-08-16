package com.nyyb.nyybserver.routine.service;

import com.nyyb.nyybserver.routine.data.repository.RoutineRepository;
import com.nyyb.nyybserver.user.data.repository.UserRepository;
import com.nyyb.nyybserver.user.service.GuestDataOwnershipTransfer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 게스트→소셜 병합 시 게스트가 만든 Routine 소유권을 소셜 유저로 이전한다.
 * KakaoService가 병합 트랜잭션 안에서 호출한다.
 */
@Component
@RequiredArgsConstructor
public class RoutineOwnershipTransfer implements GuestDataOwnershipTransfer {

    private final RoutineRepository routineRepository;
    private final UserRepository userRepository;

    @Override
    public void transfer(Long guestUserId, Long targetUserId) {
        routineRepository.transferOwner(
                userRepository.getReferenceById(guestUserId),
                userRepository.getReferenceById(targetUserId));
    }
}
