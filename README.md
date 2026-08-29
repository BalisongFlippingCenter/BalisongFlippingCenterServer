# BalisongFlippingCenter — Server

REST API backend for the BalisongFlippingCenter community platform — a full-stack web application for balisong knife enthusiasts to share content, connect with other flippers, and showcase their skills.

![Java](https://img.shields.io/badge/Java-22-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen?style=flat-square&logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?style=flat-square&logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-Containerized-blue?style=flat-square&logo=docker)
![AWS](https://img.shields.io/badge/AWS-S3%20Storage-orange?style=flat-square&logo=amazonaws)

---

## Overview

BalisongFlippingCenterServer is the Spring Boot backend that powers the BalisongFlippingCenter platform. It provides a secure REST API with JWT-based authentication, cloud media storage via AWS S3, real-time messaging over WebSocket/STOMP, and is fully containerized for consistent deployment.

**Frontend repo:** [BalisongFlippingCenterWeb](https://github.com/BalisongFlippingCenter/BalisongFlippingCenterWeb)

---

## Tech stack

| Layer | Technology |
|---|---|
| Language | Java 22 (Docker) / Java 24 (local dev) |
| Framework | Spring Boot 3.2.5 |
| Security | Spring Security + JWT (jjwt 0.11.5), 7-day refresh tokens |
| Database | PostgreSQL 16 (Spring Data JPA + Hibernate 6) |
| Schema migrations | Flyway |
| Cloud Storage | AWS SDK v1 (S3 media uploads) |
| Email | Spring Mail (JavaMailSender) |
| Real-time | Spring WebSocket + STOMP |
| Containerization | Docker (multi-stage build) |
| Orchestration | Docker Compose |
| API Docs | SpringDoc OpenAPI / Swagger UI |
| Monitoring | Spring Actuator |
| Utilities | Lombok (DTOs/records only — not JPA entities, incompatible with Java 24 annotation processing), Apache Commons IO |
| Build Tool | Maven |

---

## Features

- **JWT authentication** — stateless auth with token-based login, refresh tokens, and protected routes
- **Media uploads** — user-uploaded images and videos stored in AWS S3
- **Community content** — API endpoints for posts, profiles, knife collections, and community interaction
- **Real-time messaging** — WebSocket/STOMP-based conversations with JWT auth on CONNECT
- **Swagger UI** — interactive API documentation available at `/api/swagger-ui.html`
- **Health monitoring** — Spring Actuator endpoints for uptime and health checks
- **Dockerized** — multi-stage Docker build for lean, production-ready images
- **CI/CD** — separate GitHub Actions pipelines for staging (`test` branch) and production (`master`)

---

## Getting started

### Prerequisites

- Java 22+ (24 for local dev)
- Maven 3.9+
- Docker + Docker Compose (runs Postgres alongside the app)
- AWS account with an S3 bucket configured (for media uploads)

### Environment variables

Copy `.env.example` to `.env` and fill in real values:

```
JWT_SECRET_KEY=
AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=
AWS_S3_BUCKET=
MAIL_USERNAME=
MAIL_PASSWORD=
DB_USERNAME=
DB_PASSWORD=
ALLOWED_ORIGINS=
```

In production these are never stored in the repo or CI — they're pulled from AWS SSM Parameter Store (`/balisong/prod/*`) at container start, and S3 access there goes through the EC2 instance's IAM role rather than static keys.

### Run locally

```bash
# Clone the repo
git clone https://github.com/BalisongFlippingCenter/BalisongFlippingCenterServer.git
cd BalisongFlippingCenterServer

# Build and run with Maven (requires a running Postgres instance)
./mvnw spring-boot:run
```

### Run with Docker Compose (recommended)

Brings up Postgres and the app together, matching how production runs:

```bash
docker-compose up
```

The API is served on `http://localhost:8080`, under the `/api` context path.

---

## API documentation

Once running, interactive API docs are available via Swagger UI:

```
http://localhost:8080/api/swagger-ui.html
```

---

## Project structure

```
src/
└── main/
    └── java/
        └── com/example/BalisongFlipping/
            ├── controllers/       # REST controllers
            ├── services/          # Service interfaces
            ├── implementation/    # Service implementations
            ├── modals/            # JPA entities (explicit getters/setters, no Lombok)
            ├── dtos/              # Request/response DTOs
            ├── repositories/      # Spring Data JPA repositories
            ├── enums/             # Typed enums (knives, posts, notifications, reports)
            ├── config/            # Security, JWT filter, S3, WebSocket config
            └── utils/             # Shared helpers
```

Schema migrations live in `src/main/resources/db/migration` (Flyway).

---

## CI/CD

Two GitHub Actions pipelines, split by branch:

- **`test`** (`.github/workflows/deploy-server-to-ecr.yml`) — builds the image, pushes to ECR, and deploys over SSH to the staging EC2 host. Push or merge into `test` to update staging.
- **`master`** (`.github/workflows/deploy-server-to-prod.yml`) — builds the image, pushes to the Terraform-managed ECR repo (`balisongflippingcenter/backend/prod`), and deploys via AWS SSM `RunShellScript` (no SSH key, no long-lived AWS credentials). Authenticates to AWS via GitHub OIDC, assuming the `balisong-backend-deploy` IAM role provisioned in the [Terraform infra repo](https://github.com/BalisongFlippingCenter/BalisongFlippingCenterTerraformProd).

Promote staging to production by merging `test` into `master`.

---

## Related

- [BalisongFlippingCenterWeb](https://github.com/BalisongFlippingCenter/BalisongFlippingCenterWeb) — React/TypeScript frontend
- [BalisongFlippingCenterTerraformProd](https://github.com/BalisongFlippingCenter/BalisongFlippingCenterTerraformProd) — production AWS infrastructure (Terraform)

---

## Author

**Tyler Zenisek** — [tylerzeniseks.com](https://www.tylerzeniseks.com) · [GitHub](https://github.com/tzenisekj)
