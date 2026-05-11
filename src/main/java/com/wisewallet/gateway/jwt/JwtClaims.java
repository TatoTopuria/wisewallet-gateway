package com.wisewallet.gateway.jwt;

import java.util.List;

public record JwtClaims(
        String userId,
        String jti,
        List<String> roles,
        List<String> accountIds) {
}
