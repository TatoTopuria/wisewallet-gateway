package com.wisewallet.gateway.jwt;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "wisewallet.internal.jwt")
@Getter
@Setter
public class InternalJwtProperties {

    private String secret;
    private int ttlSeconds = 30;

    @PostConstruct
    public void validate() {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "Internal JWT secret must be at least 32 characters. Set INTERNAL_JWT_SECRET.");
        }
    }
}
