# BalisongFlippingHubServer — Claude Notes

## Project Overview
Java Spring Boot REST API backend for **Balisong Flipping Hub**, a full-stack community platform for balisong knife enthusiasts. Paired with a React/TypeScript frontend (`BalisongFlippingCenterWeb`). Both repos live under the [BalisongFlippingCenter](https://github.com/BalisongFlippingCenter) GitHub org.

- **Live frontend**: http://ec2-23-22-127-77.compute-1.amazonaws.com/ (may be down intermittently)
- **EC2 region**: us-east-1
- **ECR account**: 343218221384 → `balisongflippingcenter/backend/main`

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 22 (Docker) / Java 24 (local) |
| Framework | Spring Boot 3.2.5 |
| ORM | Spring Data JPA + Hibernate 6 |
| Database | PostgreSQL 16 (containerized via Docker) |
| Schema migrations | Flyway |
| Auth | JWT (`jjwt` 0.11.5) + refresh tokens (7-day expiry) |
| File storage | AWS S3 (`aws-java-sdk` 1.12.782) — stubbed, not yet wired |
| Email | Spring Mail — stubbed, not yet wired |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Observability | Spring Actuator (`/actuator/health`) |
| Build | Maven (mvnw wrapper) |
| Container | Docker + Docker Compose |
| CI/CD | GitHub Actions → AWS ECR |
| Hosting | AWS EC2 (manual pull + compose up) |

---

## Package Structure (`com.example.BalisongFlipping`)

```
config/           Security config, JWT filter, S3 storage config
controllers/      REST endpoints
dtos/             Request/response data transfer objects
enums/            Typed enums — knives, posts, files
implementation/   Concrete service implementations
modals/           JPA entities (explicit getters/setters — no Lombok)
repositories/     Spring Data JPA repositories
services/         Service interfaces + business logic
utils/            ProfanityFilter
```

### Controllers & Endpoints

**AuthController** (`/auth/**` — all public)
| Endpoint | Method | Purpose |
|---|---|---|
| `/auth/register` | POST | Create user — body: `{ email, displayName, password }` |
| `/auth/login` | POST | Login — body: `{ email, password }` — sets refresh cookie |
| `/auth/logout` | POST | Invalidates refresh token, expires cookie |
| `/auth/refresh-token-login` | POST | Re-auth on app load using cookie |
| `/auth/refresh-access-token` | GET | New access token using cookie |

**AccountController** (`/accounts/**` — all require Bearer token)
| Endpoint | Method | Purpose |
|---|---|---|
| `/accounts/me` | GET | Get self (UserDto) |
| `/accounts/me/change-display-name` | POST | Raw string body |
| `/accounts/me/update-bio` | POST | Raw string, max 150 chars |
| `/accounts/me/update-profile-img` | POST | Multipart — stub (501) until S3 |
| `/accounts/me/update-banner-img` | POST | Multipart — stub (501) until S3 |
| `/accounts/me/update-social-links` | POST | `{ facebookLink, twitterLink, instagramLink, youtubeLink, discordLink, redditLink, personalEmailLink, personalWebsiteLink }` |
| `/accounts/me/update-preferences` | POST | `{ measurementUnit, currency }` |
| `/accounts/me/hide-account` | POST | Toggles isHidden |
| `/accounts/me/reset-account` | POST | Wipes bio/links/images/knives/posts |
| `/accounts/me` | DELETE | Full account deletion |

### Domain Models (`modals/`)
- **accounts/**: `Account` (SINGLE_TABLE base), `User` (dtype=USER)
- **collectionKnives/**: `CollectionKnife`, `GalleryFile` (@Embeddable)
- **collections/**: `Collection`
- **posts/**: `PostWrapper` (stub), `Post`, `CollectionTimelinePost`
- **tokens/**: `RefreshToken`, `EmailVerificationToken`

### Flyway Migrations
- `V1__init.sql` — full initial schema (accounts, collections, collection_knives, gallery files, refresh_tokens, email_verification_tokens, posts)
- `V2__add_user_preferences.sql` — adds bio (varchar 150), measurement_unit, currency, is_hidden to accounts

---

## Validation Rules
- **Display name**: min 4 chars, allowed chars: letters/numbers/`!`/`.`/`_`, profanity filter (`utils/ProfanityFilter.java`)
- **Bio/Profile caption**: max 150 chars
- **Social links**: empty = clear; non-empty must start with `http://` or `https://`
- **Personal email link**: must be valid email format
- **Password**: min 7 chars

---

## Infrastructure & Deployment

### Docker Compose
Two services:
- **`postgres`**: `postgres:16-alpine`, port 5432, named volume `data`, credentials: db=`balisong_db` user=`balisong_user` pass=`balisong_pass`
- **`server`**: Spring Boot app on port 8080, depends on postgres healthy, restarts on failure

> Note: The `image:` line pointing to ECR is commented out — compose currently builds from local source (`build: .`).

### Dockerfile
Two-stage build:
1. `maven:3.9.6-eclipse-temurin-22-alpine` → builds fat jar (`mvn package -DskipTests`)
2. `eclipse-temurin:22-jre-alpine` → runs `app.jar`

### CI/CD (GitHub Actions)
Workflow: `.github/workflows/deploy-server-to-ecr.yml`
- **Trigger**: push or PR to `master`
- **Current tag**: hardcoded to `v1.1.2`
- **Secrets**: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` (in `Deployment` environment)

### DBeaver (local DB GUI)
Connect to: `localhost:5432`, db=`balisong_db`, user=`balisong_user`, pass=`balisong_pass`

---

## Lombok + Java 24 Note
Lombok annotation processing does not work with Java 24 via Maven CLI. All JPA entity classes use **explicit getters/setters** — no Lombok. Non-entity classes keep Lombok (works in IntelliJ + Docker/Java 22). Do not add `@Getter`/`@Setter` to entity classes.

---

## Known Gaps / To Do
- **S3**: `JavaFSService` is stubbed — profile/banner/collection images return `501` until S3 is wired
- **Posts**: `PostService` stubbed — deferred until after knife collection
- **Email verification**: entity + service exist but not wired into registration (deferred)
- **Change email / Change password**: deferred — requires email verification flow
- **CI/CD**: image tag hardcoded (`v1.1.2`), EC2 deploy is manual, ECR compose line commented out
- **PR YAML bug**: missing space in `- master` branch filter in workflow file
- **AWS SDK v1**: deprecation warning on startup — should upgrade to SDK v2 eventually

---

## Working Agreement
- **Always check in before making any code changes**
- All changes live on `dev` branch — not yet committed
- Backend only — frontend repo is separate (`BalisongFlippingCenterWeb`)
- Test credentials: `tzenisekj@gmail.com` / `syndicate895`
