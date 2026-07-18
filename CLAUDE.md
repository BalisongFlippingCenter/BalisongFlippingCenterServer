# BalisongFlippingHubServer — Claude Notes

## Project Overview
Java Spring Boot REST API backend for **Balisong Flipping Hub**, a full-stack community platform for balisong knife enthusiasts. Paired with a React/TypeScript frontend (`BalisongFlippingCenterWeb`). Both repos live under the [BalisongFlippingCenter](https://github.com/BalisongFlippingCenter) GitHub org.

- **Live frontend**: http://ec2-23-22-127-77.compute-1.amazonaws.com/
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
| Schema migrations | Flyway (V1–V17) |
| Auth | JWT (`jjwt` 0.11.5) + refresh tokens (7-day expiry) |
| File storage | AWS S3 (SDK v1, fully wired) |
| Email | Spring Mail (JavaMailSender, fully wired) |
| WebSocket | Spring WebSocket + STOMP (fully wired) |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Observability | Spring Actuator (`/actuator/health`) |
| Build | Maven (mvnw wrapper) |
| Container | Docker + Docker Compose |
| CI/CD | GitHub Actions → AWS ECR |
| Hosting | AWS EC2 (manual pull + compose up) |

---

## Package Structure (`com.example.BalisongFlipping`)

```
config/           Security config, JWT filter, S3 config, WebSocketConfig, WebSocketAuthInterceptor
controllers/      REST endpoints
dtos/             Request/response DTOs
enums/            Typed enums — knives, posts, notifications, reports
implementation/   Concrete service implementations
modals/           JPA entities (explicit getters/setters — no Lombok)
repositories/     Spring Data JPA repositories
services/         Service interfaces
utils/            ProfanityFilter
```

---

## Endpoint Reference

### Auth (`/auth/**` — all public except PATCH /auth/display-name)
| Method | Path | Purpose |
|---|---|---|
| POST | `/auth/register` | Register — `{ email, displayName, password }` |
| POST | `/auth/login` | Login — returns `LoginResponseDto` with `UserDto` + collection |
| POST | `/auth/logout` | Invalidate refresh token |
| POST | `/auth/refresh-token-login` | Re-auth using cookie or body token |
| GET | `/auth/refresh-access-token` | New access token from cookie |
| PATCH | `/auth/display-name` | Set initial display name (auth required) |

### Accounts (`/accounts/**`)
Public (`/accounts/any/**` — no token needed):
| Method | Path | Purpose |
|---|---|---|
| GET | `/accounts/any/{id}` | Public profile by ID |
| GET | `/accounts/any?displayName=&identifierCode=` | Public profile by handle |
| GET | `/accounts/any/search?q=` | Search users by displayName or identifierCode |
| GET | `/accounts/any/{id}/following` | Accounts this user follows (`UserSearchResultDto[]`) |
| GET | `/accounts/any/{id}/followers` | Accounts following this user (`UserSearchResultDto[]`) |
| GET | `/accounts/any/{id}/follow` | `{ following: bool }` — whether current viewer follows this account |
| POST | `/accounts/any/{id}/follow` | Follow account (auth required despite path) |
| DELETE | `/accounts/any/{id}/follow` | Unfollow account (auth required despite path) |

Auth-required (`/accounts/me/**`):
| Method | Path | Purpose |
|---|---|---|
| GET | `/accounts/me` | Self (`UserDto`) |
| POST | `/accounts/me/change-display-name` | Raw string body |
| POST | `/accounts/me/update-bio` | Raw string, max 150 chars |
| POST | `/accounts/me/update-profile-img` | Multipart |
| POST | `/accounts/me/update-banner-img` | Multipart |
| POST | `/accounts/me/update-social-links` | `UpdateSocialLinksDto` |
| POST | `/accounts/me/update-preferences` | `{ measurementUnit, currency }` |
| POST | `/accounts/me/hide-account` | Toggle isHidden |
| POST | `/accounts/me/reset-account` | Wipe bio/links/images/knives/posts |
| DELETE | `/accounts/me` | Delete account |

### Posts (`/posts/**`)
Public (`/posts/any/**`):
| Method | Path | Purpose |
|---|---|---|
| GET | `/posts/any/{id}` | Single post |
| GET | `/posts/any` | Paginated feed — filters: `postType`, `accountId`, `difficultyTag`, `search`, `page`, `size`, `knifeBladeStyle`, `knifeBladeMaterial`, `knifeBladeFinish`, `knifeHandleMaterial`, `knifeHandleConstruction`, `knifeHandleFinish`, `knifePivotSystem`, `knifePinSystem`, `knifeLatchType`, `knifeType` |

Auth-required:
| Method | Path | Purpose |
|---|---|---|
| POST | `/posts/create` | Multipart — all post types |
| GET | `/posts/me/liked` | Paginated liked posts |
| PATCH | `/posts/{id}` | Edit post (`UpdatePostDto`) |
| DELETE | `/posts/{id}` | Delete post |
| POST | `/posts/{id}/like` | Like |
| DELETE | `/posts/{id}/like` | Unlike |

### Conversations (`/conversations/**` — all auth required)
| Method | Path | Purpose |
|---|---|---|
| GET | `/conversations/me` | Inbox (`ConversationDto[]`) |
| GET | `/conversations/{id}/messages` | Paginated history — `page`, `size` |
| POST | `/conversations/{recipientId}/messages` | Send `{ body }` — creates conv if needed |
| PATCH | `/conversations/{id}/read` | Mark all read |
| DELETE | `/conversations/{id}` | Soft-delete for requester |

### Notifications (`/notifications/**` — all auth required)
| Method | Path | Purpose |
|---|---|---|
| GET | `/notifications/me` | Paginated — `unreadOnly`, `page`, `size` |
| GET | `/notifications/me/unread-count` | `{ count }` |
| PATCH | `/notifications/{id}/read` | Mark one read |
| PATCH | `/notifications/me/read-all` | Mark all read |

---

## WebSocket (STOMP)
- **Endpoint**: `/ws` (raw WebSocket, no SockJS)
- **Auth**: send `Authorization: Bearer <token>` in STOMP CONNECT headers
- **Subscriptions**:
  - `/user/me/queue/notifications` — `NotificationDto` on any notification event
  - `/user/me/queue/messages` — `MessageDto` on new incoming message
  - `/user/me/queue/conversations` — `ConversationDto` (inbox update) when a message arrives

---

## Key DTOs

**`UserDto`** — returned on login, refresh, and `/accounts/me`:
Includes: id, email, displayName, identifierCode, role, collectionId, profileImg, bannerImg, bio, social links, preferences, isHidden, `likedPostIds` (Set\<Long\>), `likedCommentIds` (Set\<Long\>), `followingIds` (Set\<Long\>), followerCount, followingCount, postCount

**`PostResponseDto`** — all post reads:
Includes: full post fields + `author (PostAuthorDto)` + `offeringKnife (PostKnifeDto)` + `referenceKnife (PostKnifeDto)`. Each `PostMedia` entry also has a `referenceKnife (PostKnifeDto)` if one was set.

**`PostKnifeDto`** — full `CollectionKnife` shape embedded in posts:
All knife attributes: bladeStyle, bladeMaterial, bladeFinish, handleMaterial, handleConstruction, handleFinish, pivotSystem, pinSystem, latchType, knifeType, overallLength, weight, msrp, all 5 scores + averageScore, hasModularBalance, balanceValue, coverPhoto.

---

## Data Model Notes
- **Posts**: SINGLE_TABLE inheritance — `PostWrapper` base, subtypes `GenericPost`, `BuySellPost`, `TradePost`, `TrickTutorialPost`, `ComboPost`
- **Post privacy**: `isPrivate` flag — excluded from public feeds but visible to owner on their own profile
- **Gallery sync**: When a post references a knife, its media files are added to that knife's gallery. Edit/delete keeps the gallery in sync automatically.
- **Conversations**: Canonical pair ordering (smaller accountId = participantA). Soft-delete per user. Email notification on first-ever message or conversation dormant > 30 days.
- **Follows**: Junction table `follows (follower_id, following_id)`. Counts maintained on User entity. `followingIds` included in `UserDto`.

---

## Infrastructure & Deployment

### Docker Compose
- **`postgres`**: `postgres:16-alpine`, port 5432, db=`balisong_db` user=`balisong_user` pass=`balisong_pass`
- **`server`**: Spring Boot on port 8080, depends on postgres healthy

### CI/CD (GitHub Actions)
- **Trigger**: push to `master`
- **Pipeline**: build → push to ECR → SSH into EC2 → pull image → `docker-compose up -d`
- **Known gap**: image tag hardcoded to `v1.1.2` in workflow

### DBeaver (local DB GUI)
`localhost:5432`, db=`balisong_db`, user=`balisong_user`, pass=`balisong_pass`

---

## Lombok + Java 24 Note
Lombok annotation processing does not work with Java 24 via Maven CLI. All JPA entity classes use **explicit getters/setters** — no Lombok. Non-entity classes (records, DTOs) are fine.

---

## Known Gaps / To Do
- **Email verification**: entity + service exist but not wired into registration flow
- **Change email / Change password**: service methods exist but deferred (require email verification)
- **CI/CD**: image tag hardcoded (`v1.1.2`), no auto-versioning
- **AWS SDK v1**: deprecation warning on startup — upgrade to SDK v2 eventually
- **Discord bot**: planned — dedicated endpoints for bug reports and flagged posts with bot auth (API key, not JWT)
- **Legal**: Privacy Policy, ToS, buy/sell + tutorial disclaimers, report/flag system — planned, not implemented

---

## Working Agreement
- **Always check in before making any code changes**
- Test credentials: `tzenisekj@gmail.com` / `syndicate895`
