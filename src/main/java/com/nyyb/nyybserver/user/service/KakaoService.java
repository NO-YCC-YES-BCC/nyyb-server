package com.nyyb.nyybserver.user.service;

import com.nyyb.nyybserver.common.response.ErrorCode;
import com.nyyb.nyybserver.common.security.AuthTokens;
import com.nyyb.nyybserver.common.security.JwtTokenProvider;
import com.nyyb.nyybserver.user.data.dto.response.KakaoTokenResponseDto;
import com.nyyb.nyybserver.user.data.dto.response.KakaoUserInfoResponseDto;
import com.nyyb.nyybserver.user.data.dto.response.SocialLoginResponseDto;
import com.nyyb.nyybserver.user.data.entity.User;
import com.nyyb.nyybserver.user.data.enums.AuthProvider;
import com.nyyb.nyybserver.user.data.exception.OAuthProcessException;
import com.nyyb.nyybserver.user.data.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KakaoService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final KakaoOAuthClient kakaoOAuthClient;
    private final RefreshTokenService refreshTokenService;
    private final List<GuestDataOwnershipTransfer> guestDataOwnershipTransfers;

    @Transactional
    public SocialLoginResponseDto createGuest() {
        User guest = userRepository.save(User.ofGuest());
        AuthTokens authTokens = jwtTokenProvider.generate(guest.getId(), guest.getRole().name());
        refreshTokenService.save(
                guest.getId(),
                authTokens.getRefreshToken(),
                jwtTokenProvider.getRefreshTokenExpireTime()
        );
        return new SocialLoginResponseDto(guest.getId(), guest.getNickname(), true, null, authTokens);
    }

    @Transactional
    public void withdraw(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found."));

        if (user.getProvider() == AuthProvider.KAKAO) {
            kakaoOAuthClient.unlink(user.getKakaoId());
        }

        refreshTokenService.delete(userId);
        userRepository.delete(user);
    }

    @Transactional
    public SocialLoginResponseDto kakaoLogin(String code, Long guestUserId) {
        if (!StringUtils.hasText(code)) {
            throw new OAuthProcessException();
        }

        KakaoUserProfile kakaoUser = getKakaoUserProfile(code);
        Optional<User> guestUser = findActiveGuest(guestUserId);
        Optional<User> socialUser = userRepository.findByProviderAndProviderId(
                AuthProvider.KAKAO,
                kakaoUser.providerId()
        );

        User user = socialUser
                .map(existingUser -> linkGuestToExistingUser(guestUser, existingUser, kakaoUser.nickname()))
                .orElseGet(() -> createOrConvertSocialUser(guestUser, kakaoUser));

        AuthTokens authTokens = jwtTokenProvider.generate(user.getId(), user.getRole().name());
        refreshTokenService.save(
                user.getId(),
                authTokens.getRefreshToken(),
                jwtTokenProvider.getRefreshTokenExpireTime()
        );
        Long linkedGuestUserId = guestUser
                .map(User::getId)
                .filter(id -> !id.equals(user.getId()))
                .orElse(null);
        return new SocialLoginResponseDto(user.getId(), user.getNickname(), false, linkedGuestUserId, authTokens);
    }

    public void logout(Long userId) {
        refreshTokenService.delete(userId);
    }

    private KakaoUserProfile getKakaoUserProfile(String code) {
        KakaoTokenResponseDto tokenResponse = kakaoOAuthClient.requestAccessToken(code);
        if (tokenResponse == null || !StringUtils.hasText(tokenResponse.getAccessToken())) {
            throw new OAuthProcessException(ErrorCode.KAKAO_API_FAILED);
        }

        KakaoUserInfoResponseDto userInfoResponse = kakaoOAuthClient.requestUserInfo(tokenResponse.getAccessToken());
        if (userInfoResponse == null || userInfoResponse.getId() == null) {
            throw new OAuthProcessException(ErrorCode.KAKAO_API_FAILED);
        }

        Long kakaoId = userInfoResponse.getId();
        return new KakaoUserProfile(kakaoId, String.valueOf(kakaoId), userInfoResponse.getNickname());
    }

    private Optional<User> findActiveGuest(Long guestUserId) {
        if (guestUserId == null) {
            return Optional.empty();
        }
        return userRepository.findByIdAndProvider(guestUserId, AuthProvider.GUEST)
                .filter(user -> !user.isMergedGuest());
    }

    private User linkGuestToExistingUser(Optional<User> guestUser, User existingUser, String nickname) {
        existingUser.syncProfile(nickname);
        guestUser.ifPresent(guest -> {
            if (!guest.getId().equals(existingUser.getId())) {
                guestDataOwnershipTransfers.forEach(transfer -> transfer.transfer(guest.getId(), existingUser.getId()));
                guest.mergeTo(existingUser);
            }
        });
        return existingUser;
    }

    private User createOrConvertSocialUser(Optional<User> guestUser, KakaoUserProfile kakaoUser) {
        if (guestUser.isPresent()) {
            User guest = guestUser.get();
            guest.connectSocial(
                    AuthProvider.KAKAO,
                    kakaoUser.kakaoId(),
                    kakaoUser.providerId(),
                    kakaoUser.nickname()
            );
            return guest;
        }

        return userRepository.save(User.ofSocial(
                AuthProvider.KAKAO,
                kakaoUser.kakaoId(),
                kakaoUser.providerId(),
                kakaoUser.nickname()
        ));
    }

    private record KakaoUserProfile(
            Long kakaoId,
            String providerId,
            String nickname
    ) {
    }
}
