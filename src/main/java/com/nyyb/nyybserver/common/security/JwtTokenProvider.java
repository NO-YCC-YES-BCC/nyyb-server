package com.nyyb.nyybserver.common.security;

import com.nyyb.nyybserver.user.data.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final Duration accessTokenValidity;
    private final Duration refreshTokenValidity;

    public JwtTokenProvider(
            @Value("${jwt.secret-key}") String secret,
            @Value("${jwt.access-token-validity-seconds:3600}") long accessTokenValiditySeconds,
            @Value("${jwt.refresh-token-validity-seconds:1209600}") long refreshTokenValiditySeconds
    ) {
        this.secretKey = Keys.hmacShaKeyFor(normalizeSecret(secret));
        this.accessTokenValidity = Duration.ofSeconds(accessTokenValiditySeconds);
        this.refreshTokenValidity = Duration.ofSeconds(refreshTokenValiditySeconds);
    }

    public AuthTokens generate(Long userId, String role) {
        Instant now = Instant.now();
        String accessToken = createToken(userId, role, now, accessTokenValidity);
        String refreshToken = createToken(userId, role, now, refreshTokenValidity);

        return new AuthTokens(accessToken, refreshToken, "Bearer", accessTokenValidity.toSeconds());
    }

    public boolean validate(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public UserPrincipal getPrincipal(String token) {
        Claims claims = parse(token).getBody();
        Long userId = Long.valueOf(claims.getSubject());
        UserRole role = UserRole.valueOf(claims.get("role", String.class));
        return new UserPrincipal(userId, role);
    }

    private String createToken(Long userId, String role, Instant issuedAt, Duration validity) {
        Instant expiresAt = issuedAt.plus(validity);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("role", role)
                .setIssuedAt(Date.from(issuedAt))
                .setExpiration(Date.from(expiresAt))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    private Jws<Claims> parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token);
    }

    private byte[] normalizeSecret(String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length >= 32) {
            return bytes;
        }

        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available.", e);
        }
    }
}
