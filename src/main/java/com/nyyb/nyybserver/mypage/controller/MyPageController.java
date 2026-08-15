package com.nyyb.nyybserver.mypage.controller;

import com.nyyb.nyybserver.common.response.GlobalResponse;
import com.nyyb.nyybserver.common.security.SecurityUtil;
import com.nyyb.nyybserver.mypage.data.dto.response.MyPageResponseDto;
import com.nyyb.nyybserver.mypage.service.MyPageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/mypage")
@Tag(name = "MyPage", description = "My page summary APIs")
public class MyPageController {

    private final MyPageService myPageService;

    // 현재 로그인 유저의 마이페이지 통계(사용하는 제품 / 덜어낸 제품 / 분석 횟수) 조회
    @Operation
    @GetMapping
    public GlobalResponse<MyPageResponseDto> getMyPage() {
        return GlobalResponse.ok(myPageService.getMyPage(SecurityUtil.getUserId()));
    }
}
