package com.nyyb.nyybserver.mypage.service;

import com.nyyb.nyybserver.analysis.data.enums.RecommendStatus;
import com.nyyb.nyybserver.analysis.data.repository.AnalysisRepository;
import com.nyyb.nyybserver.mypage.data.dto.response.MyPageResponseDto;
import com.nyyb.nyybserver.routine.data.repository.RoutineItemRepository;
import com.nyyb.nyybserver.routine.data.repository.RoutineItemSelectionRepository;
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
    private final RoutineItemSelectionRepository routineItemSelectionRepository;
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
                        // 덜어낸 제품 = 유저가 한 슬롯이라도 REMOVE로 저장한 제품 수.
                        // beforeCount - afterCount 방식은 afterCount가 슬롯 레코드 단위(BOTH 제품은 2)라
                        // 제품 수와 단위가 어긋나 실제 제외 수보다 적게 나오는 문제가 있었다.
                        routineItemSelectionRepository.countDistinctItemsByRoutineIdAndAction(
                                routine.getId(), RecommendStatus.REMOVE),
                        analysisCount
                ))
                .orElseGet(() -> new MyPageResponseDto(0, 0, analysisCount));
    }
}
