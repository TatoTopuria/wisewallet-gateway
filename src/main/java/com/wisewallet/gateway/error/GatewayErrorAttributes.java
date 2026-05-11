package com.wisewallet.gateway.error;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.util.Map;

/**
 * Customizes error attributes for Gateway-own errors (401, 429, 404 no-route).
 * Ensures `message` and `path` are always populated.
 * Removes `trace` and `exception` to avoid leaking internal details.
 */
@Component
public class GatewayErrorAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
        Map<String, Object> attributes = super.getErrorAttributes(request, options);

        // Remove sensitive fields
        attributes.remove("trace");
        attributes.remove("exception");

        // Ensure message is never null
        Object message = attributes.get("message");
        if (message == null || message.toString().isBlank()) {
            Object status = attributes.get("status");
            if (status instanceof Integer statusCode) {
                attributes.put("message", buildDefaultMessage(statusCode, request));
            }
        }

        // Ensure path is always present
        if (!attributes.containsKey("path")) {
            attributes.put("path", request.path());
        }

        return attributes;
    }

    private String buildDefaultMessage(int statusCode, ServerRequest request) {
        return switch (statusCode) {
            case 401 -> "Authentication required";
            case 403 -> "Access denied";
            case 404 -> "No route found for " + request.methodName() + " " + request.path();
            case 429 -> "Too many requests. Please slow down.";
            case 503 -> "Service temporarily unavailable. Please try again later.";
            default -> HttpStatus.resolve(statusCode) != null
                    ? HttpStatus.resolve(statusCode).getReasonPhrase()
                    : "An error occurred";
        };
    }
}
