# FinPay Microservices

FinPay is a Java Spring Boot microservices project for a digital wallet and payment platform.

## Phase 0 Status

This skeleton contains the Maven multi-module structure for:

- common-lib
- auth-service
- wallet-service
- payment-service
- notification-service
- api-gateway

## Service Ports

| Service | Port |
| --- | ---: |
| auth-service | 8081 |
| wallet-service | 8082 |
| payment-service | 8083 |
| notification-service | 8084 |
| api-gateway | 8080 |

## API Gateway Routes

| Gateway Path | Target Service |
| --- | --- |
| `/api/v1/auth/**` | auth-service |
| `/api/v1/wallets/**` | wallet-service |
| `/api/v1/wallets/*/transactions` | payment-service |
| `/api/v1/payments/**` | payment-service |
| `/api/v1/notifications/**` | notification-service |
| `/internal/v1/notifications/**` | notification-service, local testing only |

## Build

```bash
mvn clean test
```

## Infrastructure

Docker Compose includes local development services for PostgreSQL, Redis, Zookeeper, and Kafka.
The current runnable local profile uses H2, simple cache, and local event simulation. Docker-backed PostgreSQL, Redis, and Kafka integration will be added in the infrastructure phase.

## GitHub Codespaces

This repository includes a `.devcontainer` setup for Codespaces with Java 17, Maven, Docker, and Docker Compose.

After opening the repository in Codespaces:

```bash
mvn clean test
docker compose up -d
docker ps
```
