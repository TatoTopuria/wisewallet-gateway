package com.wisewallet.gateway.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Signs short-lived internal JWTs that the Gateway attaches as X-Service-Token
 * on every downstream request. Backend services validate this token instead of
 * trusting plain X-User-Id / X-User-Roles headers.
 */
@Component
public class InternalJwtSigner {

    private static final String ISSUER = "wisewallet-gateway";

    private final SecretKey signingKey;
    private final int ttlSeconds;

    public InternalJwtSigner(InternalJwtProperties properties) {
        this.signingKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.ttlSeconds = properties.getTtlSeconds();
    }

    /**
     * Creates a signed JWT carrying userId, roles, and accountIds.
     * The token expires in {@code ttlSeconds} seconds (default 30 s).
     */
    public String sign(String userId, List<String> roles, List<String> accountIds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(ISSUER)
                .subject(userId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .claim("roles", roles)
                .claim("accountIds", accountIds)
                .signWith(signingKey)
                .compact();
    }
}
