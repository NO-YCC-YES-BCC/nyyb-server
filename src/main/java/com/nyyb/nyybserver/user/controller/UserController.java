package com.nyyb.nyybserver.user.controller;

import com.nyyb.nyybserver.common.security.UserPrincipal;
import com.nyyb.nyybserver.user.data.dto.request.KakaoLoginRequestDto;
import com.nyyb.nyybserver.user.data.dto.response.SocialLoginResponseDto;
import com.nyyb.nyybserver.user.service.KakaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<SocialLoginResponseDto> guestLogin() {
        return ResponseEntity.ok(kakaoService.createGuest());
    }

    @Operation(
            summary = "Kakao login",
            description = "Logs in with Kakao. If a guest Bearer token is sent, guest-owned data is linked automatically."
    )
    @PostMapping("/kakao")
    public ResponseEntity<SocialLoginResponseDto> kakaoLogin(
            @RequestBody KakaoLoginRequestDto request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal
    ) {
        String code = request == null ? null : request.getCode();
        return ResponseEntity.ok(kakaoService.kakaoLogin(code, guestUserId(principal)));
    }

    @Operation(
            summary = "Kakao login by query code",
            description = "Compatibility endpoint for OAuth redirect. Send a guest Bearer token to link guest-owned data."
    )
    @GetMapping("/kakao")
    public ResponseEntity<SocialLoginResponseDto> kakaoLoginByQuery(
            @RequestParam String code,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(kakaoService.kakaoLogin(code, guestUserId(principal)));
    }

    private Long guestUserId(UserPrincipal principal) {
        return principal == null ? null : principal.id();
    }
}
