package com.nyyb.nyybserver.user.controller;

import com.nyyb.nyybserver.common.response.GlobalResponse;
import com.nyyb.nyybserver.common.security.SecurityUtil;
import com.nyyb.nyybserver.common.security.UserPrincipal;
import com.nyyb.nyybserver.user.data.dto.request.KakaoLoginRequestDto;
import com.nyyb.nyybserver.user.data.dto.request.KakaoNotificationRequestDto;
import com.nyyb.nyybserver.user.data.dto.response.KakaoNotificationResponseDto;
import com.nyyb.nyybserver.user.data.dto.response.SocialLoginResponseDto;
import com.nyyb.nyybserver.user.service.KakaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Guest and Kakao social login APIs")
public class UserController {
    private final KakaoService kakaoService;

    @Operation(summary = "Create guest JWT", description = "Creates a guest user and returns JWT tokens.")
    @PostMapping("/guest")
    public GlobalResponse<SocialLoginResponseDto> guestLogin() {
        return GlobalResponse.ok(kakaoService.createGuest());
    }

    @Operation(
            summary = "Kakao login",
            description = "Logs in with Kakao. If a guest Bearer token is sent, guest-owned data is linked automatically."
    )
    @PostMapping("/kakao")
    public GlobalResponse<SocialLoginResponseDto> kakaoLogin(
            @RequestBody KakaoLoginRequestDto request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal
    ) {
        String code = request == null ? null : request.getCode();
        return GlobalResponse.ok(kakaoService.kakaoLogin(code, guestUserId(principal)));
    }

    @Operation(
            summary = "Kakao login by query code",
            description = "Compatibility endpoint for OAuth redirect. Send a guest Bearer token to link guest-owned data."
    )
    @GetMapping("/kakao")
    public GlobalResponse<SocialLoginResponseDto> kakaoLoginByQuery(
            @RequestParam String code,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal
    ) {
        return GlobalResponse.ok(kakaoService.kakaoLogin(code, guestUserId(principal)));
    }

    @PostMapping("/logout")
    public GlobalResponse<Void> logout() {
        Long userId = SecurityUtil.getUserId();

        kakaoService.logout(userId);
        return GlobalResponse.ok();
    }

    @DeleteMapping("/user/me")
    public GlobalResponse<Void> withdraw() {
        Long userId = SecurityUtil.getUserId();
        kakaoService.withdraw(userId);
        return GlobalResponse.ok();
    }

    private Long guestUserId(UserPrincipal principal) {
        return principal == null ? null : principal.id();
    }

    @PatchMapping("/notify-kakao")
    public GlobalResponse<Void> updatekakoNotification(@RequestBody KakaoNotificationRequestDto kakaoNotificationRequestDto) {
        Long userId = SecurityUtil.getUserId();

        kakaoService.updateKakaoNotification(userId, kakaoNotificationRequestDto.enabled());

        return GlobalResponse.ok();
    }

    @GetMapping("/notify-kakao")
    public GlobalResponse<KakaoNotificationResponseDto> getKakaoNotification() {
        Long userId = SecurityUtil.getUserId();

        return GlobalResponse.ok(
                kakaoService.getKakaoNotification(userId)
        );
    }
}
