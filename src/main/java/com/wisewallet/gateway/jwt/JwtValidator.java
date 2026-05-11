package com.wisewallet.gateway.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtValidator {

    private final SecretKey signingKey;
    private final String expectedIssuer;

    public JwtValidator(JwtProperties properties) {
        this.signingKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.expectedIssuer = properties.getIssuer();
    }

    public JwtClaims validateAndExtract(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(expectedIssuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String userId = claims.getSubject();
            String jti = claims.getId();

            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) claims.getOrDefault("roles", List.of());

            @SuppressWarnings("unchecked")
            List<String> accountIds = (List<String>) claims.getOrDefault("accountIds", List.of());

            return new JwtClaims(userId, jti, roles, accountIds);

        } catch (ExpiredJwtException e) {
            throw new JwtValidationException("Token expired", e);
        } catch (JwtException e) {
            throw new JwtValidationException("Invalid token: " + e.getMessage(), e);
        }
    }
}
