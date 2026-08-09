# FinPay Microservices

FinPay is a Java Spring Boot microservices project for a digital wallet and payment platform.

## Live Demo

The React showcase is deployed to GitHub Pages:

**https://pratikmraut.github.io/finpay-microservices/**

The hosted site runs in demo mode with browser-local sample data, so it does not require Docker or expose a database. The login form is prefilled with the demo account. Payments made there are simulations stored only in that browser.

GitHub Pages hosts static files only. The Spring Boot services, PostgreSQL, Redis, and Kafka remain available for full-stack development through GitHub Codespaces or another backend hosting provider.

## Project Status

The Maven multi-module application contains:

- common-lib
- auth-service
- wallet-service
- payment-service
- notification-service
- api-gateway

## Service Ports

| Service | Default | Local UI |
| --- | ---: | ---: |
| auth-service | 8081 | 8181 |
| wallet-service | 8082 | 8182 |
| payment-service | 8083 | 8183 |
| notification-service | 8084 | 8184 |
| api-gateway | 8080 | 8180 |
| React frontend | 5173 | 5173 |

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
The default profile uses H2 and local event simulation. The `docker` profile uses PostgreSQL, Redis, and Kafka.

## Docker-free Frontend Demo

Run the complete frontend experience with browser-local demo data:

```bash
cd frontend
npm install
npm run dev:demo
```

Open `http://localhost:5173`. This mode does not start Java, PostgreSQL, Redis, Kafka, Zookeeper, or Docker.

Create the same production demo bundle used by GitHub Pages with:

```bash
npm run build:demo
```

## Full-stack Local Start

Docker is required for the full local infrastructure. On Windows, this command builds the project, starts FinPay PostgreSQL on port `55432`, starts Redis and Kafka, launches all five Spring applications with the `ui-local` profile, and starts React:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-local.ps1
```

Open `http://localhost:5173`. Local data is stored in the `finpay-postgres-data` Docker volume and survives application restarts.

Stop the local stack with:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop-local.ps1
```

## React Frontend

The `frontend` directory contains the FinPay React application. Start the API Gateway and backend services first, then run:

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`. Vite forwards `/api` and `/actuator` requests to the API Gateway on port `8080`.

If ports `8080`, `8081`, or `5432` are occupied by another local application, use the one-command local start. It uses ports `8180` through `8184` and PostgreSQL port `55432`.

Create a production build with:

```bash
npm run build
```

## GitHub Codespaces

This repository includes a `.devcontainer` setup for Codespaces with Java 17, Maven, Docker, and Docker Compose.

After opening the repository in Codespaces:

```bash
mvn clean test
docker compose up -d
docker ps
```

Run the Spring services against Docker infrastructure from separate terminals:

```bash
mvn -pl auth-service spring-boot:run -Dspring-boot.run.profiles=docker
mvn -pl wallet-service spring-boot:run -Dspring-boot.run.profiles=docker
mvn -pl payment-service spring-boot:run -Dspring-boot.run.profiles=docker
mvn -pl notification-service spring-boot:run -Dspring-boot.run.profiles=docker
mvn -pl api-gateway spring-boot:run
```

With the `docker` profile:

- auth, wallet, payment, and notification services use PostgreSQL
- wallet-service uses Redis-backed wallet metadata caching
- payment-service uses Redis-backed idempotency keys
- payment-service publishes payment events to Kafka
- notification-service consumes payment events from Kafka

## GitHub Pages Deployment

The workflow in `.github/workflows/deploy-pages.yml` builds `frontend` in demo mode and deploys it whenever frontend changes are pushed to `main`. In the repository settings, the Pages source must be set to **GitHub Actions**.
