package com.wisewallet.gateway.fallback;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/{service}")
    @RequestMapping("/{service}")
    public Mono<Map<String, Object>> fallback(
            @PathVariable String service,
            ServerWebExchange exchange) {

        exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);

        String originalPath = exchange.getAttribute("org.springframework.cloud.gateway.support.ServerWebExchangeUtils.gatewayRequestUrl");
        String path = originalPath != null ? originalPath : exchange.getRequest().getPath().value();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        body.put("error", HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase());
        body.put("message", "Service temporarily unavailable. Please try again later.");
        body.put("path", path);

        return Mono.just(body);
    }
}
