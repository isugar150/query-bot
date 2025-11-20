package com.namejm.query_bot.service;

import com.namejm.query_bot.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private final AppProperties appProperties;

    public TokenService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public String generateAccessToken(String username) {
        Instant now = Instant.now();
        Instant expiry = now.plus(appProperties.getSecurity().getAccessTokenMinutes(), ChronoUnit.MINUTES);
        return buildToken(username, now, expiry, "access");
    }

    public String generateRefreshToken(String username) {
        Instant now = Instant.now();
        Instant expiry = now.plus(appProperties.getSecurity().getRefreshTokenDays(), ChronoUnit.DAYS);
        return buildToken(username, now, expiry, "refresh");
    }

    public Optional<String> parseUsername(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.ofNullable(claims.getSubject());
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return "refresh".equals(claims.get("typ"));
        } catch (Exception ex) {
            return false;
        }
    }

    private String buildToken(String username, Instant issuedAt, Instant expiry, String type) {
        return Jwts.builder()
                .subject(username)
                .claim("typ", type)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiry))
                .signWith(getSigningKey())
                .compact();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(appProperties.getSecurity().getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }
}
