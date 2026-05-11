# WiseWallet — Gateway Service

API Gateway for the WiseWallet platform, built with **Spring Cloud Gateway** (WebFlux / Netty).

## Responsibilities

| Concern | Implementation |
|---|---|
| JWT validation | `JwtAuthenticationFilter` — HMAC-SHA256, Redis blocklist check |
| Rate limiting | Redis token-bucket via Spring Cloud Gateway's `RequestRateLimiter` |
| CORS | `CorsConfig` — exact origins per profile |
| Circuit breaking | Resilience4j reactive CBs per downstream service |
| Path blocking | `BlockedPathFilter` — `/internal/**`, `/actuator/**`, etc. |
| Correlation ID | `CorrelationIdFilter` — generates/propagates `X-Correlation-ID` |
| Request logging | `RequestLoggingFilter` — structured log per request/response |

## Service Coordinates

| Property | Value |
|---|---|
| App port | `8080` |
| Management port | `8090` |
| Spring runtime | Reactive (WebFlux / Netty) |
| Virtual threads | No (Netty uses event-loop threading) |

## Local Development

```bash
cp .env.example .env
# Edit .env to set JWT_SECRET (min 32 chars)

# Start infrastructure (Redis required)
docker compose --profile core up -d

# Run the service
./gradlew bootRun --args='--spring.profiles.active=local'
```

## Environment Variables

See `.env.example` for all required environment variables.

| Variable | Required | Description |
|---|---|---|
| `JWT_SECRET` | Yes | HMAC-SHA256 signing key (min 32 chars) |
| `REDIS_HOST` | Yes | Redis host (default: localhost) |
| `REDIS_PORT` | No | Redis port (default: 6379) |
| `ACCOUNT_SERVICE_URL` | Yes | Base URL of Account Service |
| `TRANSACTION_SERVICE_URL` | Yes | Base URL of Transaction Service |
| `NOTIFICATION_SERVICE_URL` | Yes | Base URL of Notification Service |
| `ADVISOR_SERVICE_URL` | Yes | Base URL of Advisor Service |

## Route Table

| Route | Method | Forwarded to | JWT Required |
|---|---|---|---|
| `/api/auth/register` | POST | account-service | No |
| `/api/auth/login` | POST | account-service | No |
| `/api/auth/refresh` | POST | account-service | No |
| `/api/auth/logout` | POST | account-service | Yes |
| `/api/accounts/**` | Any | account-service | Yes |
| `/api/transactions/**` | Any | transaction-service | Yes |
| `/api/notifications/**` | Any | notification-service | Yes |
| `/api/advisor/**` | Any | advisor-service | Yes |

Blocked paths (403): `/internal/**`, `/actuator/**`, `/h2-console/**`, `/swagger-ui/**`, `/v3/api-docs/**`

## Headers Forwarded to Downstream Services

| Header | Value |
|---|---|
| `X-User-Id` | JWT `sub` claim (userId UUID) |
| `X-User-Roles` | JWT `roles` claim (comma-separated) |
| `X-Account-Ids` | JWT `accountIds` claim (comma-separated UUIDs) |
| `X-Correlation-ID` | Generated or propagated correlation ID |

The `Authorization: Bearer ...` header is **stripped** before forwarding.

## Health & Metrics

- Health: `GET http://localhost:8090/actuator/health`
- Prometheus: `GET http://localhost:8090/actuator/prometheus`
- Circuit breakers: `GET http://localhost:8090/actuator/circuitbreakers`
