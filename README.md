# BalisongFlippingHub — Server

REST API backend for the BalisongFlippingHub community platform — a full-stack web application for balisong knife enthusiasts to share content, connect with other flippers, and showcase their skills.

![Java](https://img.shields.io/badge/Java-22-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen?style=flat-square&logo=springboot)
![MongoDB](https://img.shields.io/badge/MongoDB-Database-green?style=flat-square&logo=mongodb)
![Docker](https://img.shields.io/badge/Docker-Containerized-blue?style=flat-square&logo=docker)
![AWS](https://img.shields.io/badge/AWS-S3%20Storage-orange?style=flat-square&logo=amazonaws)

---

## Overview

BalisongFlippingHubServer is the Spring Boot backend that powers the BalisongFlippingHub platform. It provides a secure REST API with JWT-based authentication, cloud media storage via AWS S3, and is fully containerized for consistent deployment.

**Frontend repo:** [BalisongFlippingHubWeb](https://github.com/BalisongFlippingHub/BalisongFlippingHubWeb)

---

## Tech stack

| Layer | Technology |
|---|---|
| Language | Java 22 |
| Framework | Spring Boot 3.2.5 |
| Security | Spring Security + JWT (jjwt 0.11.5) |
| Database | MongoDB (Spring Data MongoDB) |
| Cloud Storage | AWS SDK (S3 media uploads) |
| Containerization | Docker (multi-stage build) |
| Orchestration | Docker Compose |
| API Docs | SpringDoc OpenAPI / Swagger UI |
| Monitoring | Spring Actuator |
| Utilities | Lombok, Apache Commons IO |
| Build Tool | Maven |

---

## Features

- **JWT authentication** — stateless auth with token-based login and protected routes
- **Media uploads** — user-uploaded images and videos stored in AWS S3
- **Community content** — API endpoints for posts, profiles, and community interaction
- **Swagger UI** — interactive API documentation available at `/swagger-ui.html`
- **Health monitoring** — Spring Actuator endpoints for uptime and health checks
- **Dockerized** — multi-stage Docker build for lean, production-ready images
- **CI/CD** — automated build and deployment pipeline via GitHub Actions

---

## Getting started

### Prerequisites

- Java 22+
- Maven 3.9+
- MongoDB instance (local or Atlas)
- AWS account with S3 bucket configured
- Docker (optional, for containerized setup)

### Environment variables

Create an `application.properties` or set the following environment variables:

```
MONGODB_URI=your_mongodb_connection_string
JWT_SECRET=your_jwt_secret_key
AWS_ACCESS_KEY_ID=your_aws_access_key
AWS_SECRET_ACCESS_KEY=your_aws_secret_key
AWS_S3_BUCKET=your_s3_bucket_name
AWS_REGION=your_aws_region
```

### Run locally

```bash
# Clone the repo
git clone https://github.com/BalisongFlippingHub/BalisongFlippingHubServer.git
cd BalisongFlippingHubServer

# Build and run with Maven
./mvnw spring-boot:run
```

The server starts on `http://localhost:8080` by default.

### Run with Docker

```bash
# Build the image
docker build -t balisong-server .

# Run the container
docker run -p 8080:8080 --env-file .env balisong-server
```

### Run with Docker Compose

```bash
docker-compose up
```

---

## API documentation

Once running, interactive API docs are available via Swagger UI:

```
http://localhost:8080/swagger-ui.html
```

---

## Project structure

```
src/
└── main/
    └── java/
        └── com/example/BalisongFlipping/
            ├── controllers/    # REST controllers
            ├── services/       # Business logic
            ├── models/         # MongoDB document models
            ├── repositories/   # Spring Data repositories
            ├── security/       # JWT filters and Spring Security config
            └── config/         # App configuration (AWS, CORS, etc.)
```

---

## Related

- [BalisongFlippingHubWeb](https://github.com/BalisongFlippingHub/BalisongFlippingHubWeb) — React/TypeScript frontend

---

## Author

**Tyler Zenisek** — [tylerzeniseks.com](https://www.tylerzeniseks.com) · [GitHub](https://github.com/tzenisekj)
