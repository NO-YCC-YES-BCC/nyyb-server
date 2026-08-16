package com.nyyb.nyybserver.mypage.service;

import com.nyyb.nyybserver.analysis.data.repository.AnalysisRepository;
import com.nyyb.nyybserver.mypage.data.dto.response.MyPageResponseDto;
import com.nyyb.nyybserver.routine.data.entity.Routine;
import com.nyyb.nyybserver.routine.data.repository.RoutineItemRepository;
import com.nyyb.nyybserver.routine.data.repository.RoutineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MyPageService {

    private final RoutineRepository routineRepository;
    private final RoutineItemRepository routineItemRepository;
    private final AnalysisRepository analysisRepository;

    /**
     * 마이페이지 통계 조회.
     * 제품 관련 수치는 가장 최신 루틴 기준이며, 루틴이 아직 없으면 0으로 내려간다.
     * @param userId 현재 로그인 유저 id (게스트/카카오 공통)
     * @return 사용하는 제품 수 + 덜어낸 제품 수 + 분석 횟수
     */
    @Transactional(readOnly = true)
    public MyPageResponseDto getMyPage(Long userId) {
        long analysisCount = analysisRepository.countByUserId(userId);

        return routineRepository.findFirstByUserIdOrderByCreatedAtDescIdDesc(userId)
                .map(routine -> new MyPageResponseDto(
                        routineItemRepository.countByRoutineId(routine.getId()),
                        countRemoved(routine),
                        analysisCount
                ))
                .orElseGet(() -> new MyPageResponseDto(0, 0, analysisCount));
    }

    // 덜어낸 제품 = 분석 전 제품 개수 - 유저가 유지 선택한 개수.
    // 아직 저장 전이라 afterCount가 없으면 덜어낸 제품이 없는 것으로 보고, 음수는 0으로 막는다.
    private long countRemoved(Routine routine) {
        int before = routine.getBeforeCount() != null ? routine.getBeforeCount() : 0;
        int after = routine.getAfterCount() != null ? routine.getAfterCount() : before;
        return Math.max(0, before - after);
    }
}
