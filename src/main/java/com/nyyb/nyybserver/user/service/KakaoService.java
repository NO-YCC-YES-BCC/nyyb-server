package com.nyyb.nyybserver.user.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nyyb.nyybserver.common.security.AuthTokens;
import com.nyyb.nyybserver.common.security.JwtTokenProvider;
import com.nyyb.nyybserver.user.data.dto.response.KakaoNotificationResponseDto;
import com.nyyb.nyybserver.user.data.dto.response.SocialLoginResponseDto;
import com.nyyb.nyybserver.user.data.entity.User;
import com.nyyb.nyybserver.user.data.enums.AuthProvider;
import com.nyyb.nyybserver.user.data.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KakaoService {
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final List<GuestDataOwnershipTransfer> guestDataOwnershipTransfers;
    private final RestClient restClient = RestClient.create();

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.client-secret:}")
    private String clientSecret;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    @Transactional
    public SocialLoginResponseDto createGuest() {
        User guest = userRepository.save(User.ofGuest());
        AuthTokens authTokens = jwtTokenProvider.generate(guest.getId(), guest.getRole().name());
        return new SocialLoginResponseDto(guest.getId(), guest.getNickname(), true, null, authTokens);
    }

    @Transactional
    public SocialLoginResponseDto kakaoLogin(String code, Long guestUserId) {
        if (!StringUtils.hasText(code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kakao authorization code is required.");
        }

        KakaoTokenResponse tokenResponse = requestAccessToken(code);
        if (tokenResponse == null || !StringUtils.hasText(tokenResponse.accessToken())) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Kakao token response is empty.");
        }

        KakaoUserResponse kakaoUser = requestUserInfo(tokenResponse.accessToken());
        Optional<User> guestUser = findActiveGuest(guestUserId);
        Optional<User> socialUser = userRepository.findByProviderAndProviderId(AuthProvider.KAKAO, kakaoUser.providerId());

        User user = socialUser
                .map(existingUser -> linkGuestToExistingUser(guestUser, existingUser, kakaoUser.nickname()))
                .orElseGet(() -> createOrConvertSocialUser(guestUser, kakaoUser));

        AuthTokens authTokens = jwtTokenProvider.generate(user.getId(), user.getRole().name());
        Long linkedGuestUserId = guestUser
                .map(User::getId)
                .filter(id -> !id.equals(user.getId()))
                .orElse(null);
        return new SocialLoginResponseDto(user.getId(), user.getNickname(), false, linkedGuestUserId, authTokens);
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

    private User createOrConvertSocialUser(Optional<User> guestUser, KakaoUserResponse kakaoUser) {
        if (guestUser.isPresent()) {
            User guest = guestUser.get();
            guest.connectSocial(AuthProvider.KAKAO, kakaoUser.kakaoId(), kakaoUser.providerId(), kakaoUser.nickname());
            return guest;
        }

        return userRepository.save(User.ofSocial(
                AuthProvider.KAKAO,
                kakaoUser.kakaoId(),
                kakaoUser.providerId(),
                kakaoUser.nickname()
        ));
    }

    private KakaoTokenResponse requestAccessToken(String code) {
        LinkedMultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("redirect_uri", redirectUri);
        body.add("code", code);
        if (StringUtils.hasText(clientSecret)) {
            body.add("client_secret", clientSecret);
        }

        try {
            return restClient.post()
                    .uri("https://kauth.kakao.com/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(KakaoTokenResponse.class);
        } catch (RestClientResponseException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Kakao authorization code is invalid or expired.",
                    e
            );
        }
    }

    private KakaoUserResponse requestUserInfo(String accessToken) {
        KakaoUserInfoResponse response;
        try {
            response = restClient.get()
                    .uri("https://kapi.kakao.com/v2/user/me")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(KakaoUserInfoResponse.class);
        } catch (RestClientResponseException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to request Kakao user info.",
                    e
            );
        }

        if (response == null || response.id() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Kakao user response is empty.");
        }

        String nickname = extractNickname(response);
        return new KakaoUserResponse(response.id(), String.valueOf(response.id()), nickname);
    }

    private String extractNickname(KakaoUserInfoResponse response) {
        Map<String, Object> properties = response.properties();
        Map<String, Object> kakaoAccount = response.kakaoAccount();
        Map<String, Object> profile = nestedMap(kakaoAccount, "profile");

        String nickname = stringValue(profile, "nickname");
        if (!StringUtils.hasText(nickname)) {
            nickname = stringValue(properties, "nickname");
        }
        return StringUtils.hasText(nickname) ? nickname : "Kakao User";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> nestedMap(Map<String, Object> source, String key) {
        Object value = source == null ? null : source.get(key);
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : Map.of();
    }

    private String stringValue(Map<String, Object> source, String key) {
        Object value = source == null ? null : source.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private record KakaoTokenResponse(
            @JsonProperty("access_token") String accessToken
    ) {
    }
    private record KakaoUserInfoResponse(
            Long id,
            Map<String, Object> properties,
            @JsonProperty("kakao_account") Map<String, Object> kakaoAccount
    ) {
    }

    private record KakaoUserResponse(
            Long kakaoId,
            String providerId,
            String nickname
    ) {
    }

    @Transactional
    public void updateKakaoNotification(Long userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        user.updateKakaoNotification(enabled);
    }

    @Transactional(readOnly = true)
    public KakaoNotificationResponseDto getKakaoNotification(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return new KakaoNotificationResponseDto(user.getNotifyKakao());
    }
}
