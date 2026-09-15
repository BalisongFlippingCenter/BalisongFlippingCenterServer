# BalisongFlippingHubServer — Claude Notes

## Project Overview
Java Spring Boot REST API backend for **Balisong Flipping Hub**, a full-stack community platform for balisong knife enthusiasts. Paired with a React/TypeScript frontend (`BalisongFlippingCenterWeb`). Both repos live under the [BalisongFlippingCenter](https://github.com/BalisongFlippingCenter) GitHub org.

- **Live frontend (production)**: https://www.balisongflippingcenter.com — served via CloudFront + S3, not an EC2 instance
- **Staging frontend**: http://ec2-23-22-127-77.compute-1.amazonaws.com/ — standalone nginx container on its own EC2 instance, unrelated to the CloudFront/S3 production path, but points at the same production backend/DB (see below)
- **EC2 region**: us-east-1
- **ECR account**: 343218221384 → `balisongflippingcenter/backend/prod`

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
| File storage | AWS S3 (SDK v2 — `S3Client` + `S3Presigner`, fully wired) |
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
| POST | `/auth/verify-admin-login` | `{ email, code }` — completes login for `ADMIN` accounts (see below) |
| GET | `/auth/verify-email-token/{token}` | Completes email verification for a new password-based account |
| POST | `/auth/resend-email-token/{email}` | Re-sends (replaces) the 6-digit verification code |

**Admin login step-up**: when `POST /auth/login` succeeds for an account with `role=ADMIN`, no tokens are issued yet — a 6-digit code (reusing the `EmailVerificationToken`/`email_verification_tokens` mechanism, 10-min expiry, same table password-reset uses) is emailed to the account and the response is `202` with `AdminLoginChallengeDto{requiresAdminVerification:true, email}` instead of `LoginResponseDto`. The client then calls `POST /auth/verify-admin-login` with the code to receive the normal `LoginResponseDto` (access token, refresh-token cookie, etc.) exactly as a non-admin login would. A second "login successful" email fires once verification succeeds, as a canary. Google sign-in (`/auth/google`) is blocked entirely for `ADMIN_BOOTSTRAP_EMAIL` (both logging into an existing account and creating a new one with that address) — password is the only login path for that account, so there's one auth path to secure rather than two, and no way for someone else to claim that email via Google before the real admin registers it. Non-admin logins are unaffected.

**Email verification (password-based accounts)**: every account created via `POST /auth/register` starts with `emailVerified=false` and gets a 6-digit code emailed immediately (same `EmailVerificationToken` mechanism as admin step-up). `POST /auth/login` rejects an unverified account with `409` and the plain-text body `"Please verify your email before logging in."` (never issues tokens) until `GET /auth/verify-email-token/{token}` succeeds. Google sign-in (`/auth/google`) sets `emailVerified=true` at account creation and is entirely unaffected — Google already verifies the email as part of OAuth, so there's nothing to re-verify. The frontend's `RegisterVerifyPage`/`VerificationTokenInput` (`/register/verify/:email`) handles code entry and resend for both the post-registration redirect and the login-blocked case.

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
| POST | `/accounts/me/request-email-change` | Emails a 6-digit code to the *current* email |
| POST | `/accounts/me/confirm-email-change` | `{ code, newEmail }` — validates the code, then applies the new email |
| POST | `/accounts/me/request-password-change` | Emails a 6-digit code to the account's email |
| POST | `/accounts/me/confirm-password-change` | `{ code, newPassword }` — validates the code, then applies the new password |

Both change flows reuse the `EmailVerificationToken` mechanism (10-min expiry) — `request*` deletes any existing token for the account before issuing a new one, so `request*` must be `@Transactional` (a derived-delete repository call outside a transaction throws "No EntityManager with actual transaction available"; this bit both request methods until fixed). Frontend: `ProfileConfigurationChangeEmailPage`/`ChangePasswordPage` (`/configure/email`, `/configure/password`), linked from the Account section of Settings.

### Posts (`/posts/**`)
Public (`/posts/any/**`):
| Method | Path | Purpose |
|---|---|---|
| GET | `/posts/any/{id}` | Single post |
| GET | `/posts/any` | Paginated feed — filters: `postType`, `accountId`, `difficultyTag`, `search`, `page`, `size`, `knifeBladeStyle`, `knifeBladeMaterial`, `knifeBladeFinish`, `knifeHandleMaterial`, `knifeHandleConstruction`, `knifeHandleFinish`, `knifePivotSystem`, `knifePinSystem`, `knifeLatchType`, `knifeType` |

Auth-required:
| Method | Path | Purpose |
|---|---|---|
| POST | `/posts/upload-url` | JSON — request presigned S3 PUT URLs for post media (`postType`, `files[]`) |
| POST | `/posts/create` | JSON (`CreatePostRequestDto`) — media is a list of already-uploaded `{url, description, referenceKnifeId}`, not raw files |
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

### Reports & Moderation (`/reports/**`)
| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/reports` | any authed user | `CreateReportDto{targetType, targetId, reason, additionalNote}` — targets `POST`/`COMMENT`/`PROFILE`/`CONVERSATION`/`MESSAGE`; reason set is validated per target type (see `ReportService`); dedupes and blocks self-reporting profiles |
| GET | `/reports` | `ADMIN` | Paginated queue — filters: `status`, `targetType`, `page`, `size` |
| PATCH | `/reports/{id}/status` | `ADMIN` | `{ status }` — `PENDING`/`REVIEWED`/`DISMISSED`/`ACTIONED`. `ACTIONED` is a record-keeping label only — it does not itself delete/mutate the target; the admin takes the actual action (e.g. delete post) via the normal endpoints |

`ModerationService` auto-resolves `PROFILE` reports for `INAPPROPRIATE_NAME`/`INAPPROPRIATE_BIO`: if the profanity filter confirms it, the name is reset / bio cleared automatically, the user is emailed + notified, and the report is closed without human review. Everything else sits `PENDING` for an admin. `CONVERSATION`/`MESSAGE` reports currently skip reason-set validation (no `POST_REASONS`-equivalent set defined for them yet).

### Account Enforcement (`/admin/accounts/**` — all `ADMIN`)
| Method | Path | Purpose |
|---|---|---|
| GET | `/admin/accounts/search?q=` | Search by email, displayName, or identifierCode |
| GET | `/admin/accounts/{id}` | One account's moderation status |
| POST | `/admin/accounts/{id}/ban` \| `/unban` | `{reason}` — permanent login lockout |
| POST | `/admin/accounts/{id}/suspend` \| `/unsuspend` | `{reason, until}` (ISO-8601 instant) — temporary login lockout, auto-lifts at `until` |
| POST | `/admin/accounts/{id}/mute` \| `/unmute` | `{reason, until}` — blocks creating posts/comments only; login and browsing are unaffected |

Ban/suspend are enforced two ways: at login (`AuthServiceImplementation.authenticate` checks before password verification, throwing `DisabledException`/`LockedException` with the reason and, for suspensions, the expiry — caught in `AuthController` and returned as `409`), and on every subsequent request (`JwtAuthFilter` re-checks `isEnabled()`/`isAccountNonLocked()` on the freshly-loaded account before honoring an otherwise-valid access token, so revocation is immediate rather than waiting for the token to expire — returns `403`). Mute is checked at the top of `PostService.createPost` and `CommentService.createComment` only. Admin accounts can never be targeted (`AdminAccountService.requireModerable` rejects with `409`). Frontend: `AdminAccountsPage` (`/admin/accounts`, linked from the sidebar) — search, then expand a result to ban/suspend/mute with a reason (and duration for the latter two).

**Becoming an admin**: `role` is a plain string on `Account` (`"USER"` by default), turned into `ROLE_<value>` for Spring Security, re-derived from the DB on every request (not cached in the JWT — a role change takes effect on the very next request). There's no promote-another-admin endpoint yet. The first admin is set via `AdminBootstrapRunner` (`config/`): it checks on startup **and every 3 minutes thereafter** (`@Scheduled`, requires `@EnableScheduling` on `BalisongFlippingApplication`) whether `ADMIN_BOOTSTRAP_EMAIL` is set and no account currently has `role=ADMIN` — if so, it promotes the matching account. The recurring check exists so a wiped/restored DB self-heals admin access (register the account again, wait up to 3 min) without needing a backend restart. It's a no-op forever once any admin exists, so it's safe to leave enabled — treat it as a "restore my admin access" safety net, not a way to add a second admin later.

### Notifications (`/notifications/**` — all auth required)
| Method | Path | Purpose |
|---|---|---|
| GET | `/notifications` | Paginated — `unreadOnly`, `page`, `size` |
| GET | `/notifications/unread-count` | Raw number, not wrapped (e.g. `0`, not `{count: 0}`) |
| PATCH | `/notifications/{id}/read` | Mark one read |
| PATCH | `/notifications/read-all` | Mark all read |

### Catalog (`/catalog/**` public, `/admin/catalog/**` all `ADMIN`)
| Method | Path | Purpose |
|---|---|---|
| GET | `/catalog/any/knives?search=` | List/search knives (`KnifeSummaryDto[]`) |
| GET | `/catalog/any/knives/{slug}` | Full detail (`KnifeDetailDto` — versions → variants) |
| GET | `/catalog/any/makers` | List makers |
| GET | `/catalog/any/makers/{slug}` | Full maker detail incl. its knives |
| POST | `/admin/catalog/upload-url` | Presigned S3 PUT URL for a cover or variant image |
| POST | `/admin/catalog/import` | Bulk upsert-by-slug — `{makers?, knives?}`, same shape as the seed JSON |
| POST/PUT/DELETE | `/admin/catalog/makers[/{slug}]` | Maker CRUD |
| POST/PUT/DELETE | `/admin/catalog/knives[/{slug}]` | Knife CRUD — body is a full `KnifeSeedDto` (knife → versions → variants), same shape `/import` takes for one knife |

**Shape**: `Knife` → `KnifeVersion` (one per real hardware/material revision, e.g. V3 vs a titanium remake — cosmetic-only colorway SKUs are *not* separate versions) → `KnifeVariant` (one per blade type offered under that version: `TRAINER` or a `LIVE_BLADE` style like Tanto/Bowie/Weehawk).

**Required fields** (`CatalogSeedService.validateRequiredFields`, shared by create/update/import — 400 `CatalogValidationException` listing exactly what's missing): every version needs `overallLength`, `weight`, `pivotSystem`, `latchType`, `pinSystem`, `handleConstruction`, `handleMaterial`, `handleFinish`; every variant needs `msrp`; `LIVE_BLADE` variants additionally need `bladeStyle` and `bladeMaterial` (optional-but-settable for `TRAINER`, since a trainer's steel is generic and not a meaningful spec). `bladeFinish` is not tracked anywhere on the catalog side — cosmetic-only, deliberately dropped (migration V24); this is an info page, not a store, so finish doesn't carry the way material/style do. Plastic handle/blade materials get specific values (`CPVC`, `ACETAL`, `ULTEM`, `HDPE`) alongside the generic `PLASTIC`, mirroring how aluminum has `ALUMINIUM_6061`/`ALUMINIUM_7075` alongside generic `ALUMINIUM` — added after the Squiddy line (4 different plastics across 6 catalog entries) exposed the generic bucket as too coarse.

**Cover/variant images**: direct-to-S3 upload flow exists and works (`/admin/catalog/upload-url` + `S3Presigner`), but no catalog entries have real images yet — sourcing was put on hold pending copyright: rehosting manufacturer/retailer product photos onto the app's own S3 bucket is a real exposure for a public platform. Options identified but not pursued: ask makers for press-kit images, use photos of knives actually owned, or eventually backfill from user-submitted post photos.

**Squid Industries** (`squid-industries` maker) is the only maker with real catalog entries so far — 10 knives: Krake Raken + Titanium Krake Raken (each deeply spec-verified against official + retailer sources, multiple correction passes), Squiddy/-B/-U/-WH/-A/-XL (same), Squidtrainer and Mako (only lightly verified during the original bulk import — still need the same fact-by-fact re-verification pass the others got).

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

## Media Upload Flow (direct-to-S3)

Post media (`/posts/create`) and new-knife gallery media (`/collection/me/add-knife`) upload **directly from the browser to S3** — the backend is never in the byte path for these. Single-image fields (profile img, banner img, collection banner, knife cover photo) still upload through the backend the old way (small enough not to matter).

1. Client calls `POST /posts/upload-url` (or `POST /collection/me/knife-gallery-upload-url`) with `{filename, contentType}` per file. Backend returns a presigned S3 `PUT` URL (`S3Presigner`, 10-min expiry) + the final public URL per file, using the same key-namespacing scheme as before (`posts/{accountId}/{postType}/{uuid}/{filename}`, `collection-knives/{collectionId}/{safeDisplayName}/gallery/{filename}`).
2. Client `PUT`s each file straight to S3 with `Content-Type` matching what was requested (SigV4 signs `Content-Type`, so a mismatch fails the signature).
3. Client calls `/posts/create` or `/collection/me/add-knife` with the resulting URLs instead of raw files.
4. Backend validates every URL before persisting: the S3 key must be under the caller's own `accountId`/`collectionId` prefix (`PostService.validateOwnedUploadedKey`, inline in `CollectionController`), and a `HeadObject` check (`S3Service.doesObjectExist`) confirms the upload actually landed. `isVideo` is derived server-side from the URL's file extension, not trusted from the client.
5. Orphaned S3 objects (uploaded via step 2, post/knife never created) are not cleaned up — accepted as a low storage cost for now.

S3 bucket CORS (`PUT` from the frontend origins) is required for step 2 and lives in the `BalisongFlippingCenterTerraformProd` repo (`s3.tf`, `aws_s3_bucket_cors_configuration.app_uploads`) — apply it there, not from this repo.

## Data Model Notes
- **Posts**: SINGLE_TABLE inheritance — `PostWrapper` base, subtypes `GenericPost`, `BuySellPost`, `TradePost`, `TrickTutorialPost`, `ComboPost`
- **Post privacy**: `isPrivate` flag — excluded from public feeds but visible to owner on their own profile
- **Gallery sync**: When a post references a knife, its media files are added to that knife's gallery. Edit/delete keeps the gallery in sync automatically.
- **Conversations**: Canonical pair ordering (smaller accountId = participantA). Soft-delete per user. Email notification on first-ever message or conversation dormant > 30 days.
- **Follows**: Junction table `follows (follower_id, following_id)`. Counts maintained on User entity. `followingIds` included in `UserDto`.

---

## Infrastructure & Deployment

### Docker Compose (local dev)
- **`postgres`**: `postgres:16-alpine`, port 5432, db=`balisong_db` user=`balisong_user` pass=`balisong_pass`
- **`server`**: Spring Boot on port 8080, depends on postgres healthy

### DBeaver (local DB GUI)
`localhost:5432`, db=`balisong_db`, user=`balisong_user`, pass=`balisong_pass`

### Production infrastructure (Terraform-managed, since 2026-08-24)
IaC lives in a separate repo, `BalisongFlippingCenterTerraformProd` (not cloned locally by default — `gh repo clone BalisongFlippingCenter/BalisongFlippingCenterTerraformProd`). S3 backend for state: `balisong-flipping-center-terraform-state-prod`.

- **Production frontend**: CloudFront (`d2zzr8bab26vq8.cloudfront.net`, aliases `balisongflippingcenter.com` / `www.balisongflippingcenter.com`) serving an S3 bucket (`balisong-flipping-center-frontend-prod`) via OAC. `/api/*` is routed by an `ordered_cache_behavior` to the backend EC2 origin (caching disabled); everything else hits the S3 origin with a CloudFront Function rewriting SPA routes to `index.html`.
- **Production backend**: EC2 instance `i-0fd15131d9c0681e8` (`100.61.159.104`), created by Terraform's `aws_instance.server` + `aws_eip.server`. Security group restricts port 8080 to CloudFront's managed prefix list only, SSH to a single admin CIDR (`var.ssh_allowed_cidr`).
  - `user_data` (see `templates/user_data.sh.tpl`) installs Docker/Compose/AWS CLI/SSM agent, pulls all app secrets from SSM Parameter Store (`/balisong/prod/*`) into `.env`, writes `docker-compose.yaml` from a template baked into `user_data`, logs into ECR, and starts the stack — so the instance is fully reproducible from a `terraform apply`, with no hand-maintained config on the box.
  - Secrets (`DB_PASSWORD`, `JWT_SECRET_KEY`, `MAIL_USERNAME`, `MAIL_PASSWORD`) are Terraform variables supplied via `TF_VAR_*` env vars at apply time (never written to `terraform.tfvars`), stored as SSM `SecureString`/`String` parameters, and read by the instance role at boot — the app never gets long-lived AWS access keys (S3 access is via the instance's IAM role).
  - `ADMIN_BOOTSTRAP_EMAIL` (see Reports & Moderation above) needs to be added to this same SSM/`user_data` pipeline in `BalisongFlippingCenterTerraformProd` before it'll take effect in prod — not yet done as of this writing. Also needs adding to the testing box's own `.env` by hand (it's outside Terraform).
- **Testing/staging backend**: EC2 instance `i-0638063ca847f2274` (`3.217.173.234`, tagged `balisong-testing-server`) — the *original* manually-created box, kept intentionally outside Terraform as an isolated test environment with its own Postgres, decoupled from real user data. Its security group (`launch-wizard-2`) is still wide open (22/80/8080 to `0.0.0.0/0`) — fine for a personal test box, but tighten if that ever changes.
- **CI/CD (GitHub Actions, `.github/workflows/deploy-server-to-prod.yml`)**: on push to `master` — build/push image to ECR (`AWS_ECR_REPOSITORY_URL` repo var, tagged with the commit SHA and `latest`) via OIDC (`AWS_BACKEND_DEPLOY_ROLE_ARN`), deploy via `aws ssm send-command` (`docker-compose pull server && up -d --force-recreate server`) against `AWS_EC2_INSTANCE_ID`, then poll `https://balisongflippingcenter.com/api/actuator/health` for up to 5 minutes to verify.
  - `AWS_EC2_INSTANCE_ID` must always match whichever instance ID Terraform currently outputs for `aws_instance.server` (`terraform output ec2_instance_id`) — it does **not** update itself. A 2026-08-24 incident traced back to this variable silently pointing at a Terraform instance that had been replaced (likely by the AMI-forced-replacement scenario noted in `ec2.tf`) and never updated, so deploys had been SSM-targeting a nonexistent instance while a manually-created replacement (now `balisong-testing-server`) served real production traffic with hand-edited, buggy config. Always cross-check this variable against `terraform output` after any `apply` that touches `aws_instance.server`.

---

## Lombok + Java 24 Note
Lombok annotation processing does not work with Java 24 via Maven CLI. All JPA entity classes use **explicit getters/setters** — no Lombok. Non-entity classes (records, DTOs) are fine.

---

## Known Gaps / To Do
- **Discord bot**: planned — dedicated endpoints for bug reports and flagged posts with bot auth (API key, not JWT)
- **Legal**: Privacy Policy, ToS, buy/sell + tutorial disclaimers — planned, not implemented

---

## Working Agreement
- **Always check in before making any code changes**
