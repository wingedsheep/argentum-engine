# OAuth2 Accounts & Persistent Identity

A direct OAuth2 / OIDC + Spring Security implementation that replaces the current ephemeral
`PlayerIdentity` (random UUID in `localStorage`) with persistent user accounts backed by Postgres,
and starts recording per-account game outcomes so logged-in users can see their win/loss record
over time. **Google** is the first wired-up provider; **GitHub** and **Microsoft** are
designed-for follow-ups that drop in via a new `ClientRegistration` and a per-provider claim
mapper. Anonymous guest play stays intact for casual games and dev scenario E2E tests.

## Why direct OAuth (not Keycloak)

We use direct OAuth2 / OIDC integration via `spring-boot-starter-oauth2-client` /
`spring-boot-starter-oauth2-resource-server` rather than fronting providers with Keycloak.

| Concern              | Keycloak                                            | Direct OAuth                                        |
|----------------------|-----------------------------------------------------|-----------------------------------------------------|
| Infra cost           | Extra service to deploy, upgrade, back up           | None — the IdPs do the work                         |
| Dev ergonomics       | Docker Compose service + realm/client config        | One env-var pair per provider                       |
| User experience      | Provider login via intermediate Keycloak redirect   | One redirect, no extra branding                     |
| Multi-provider later | Trivial (Keycloak federates Discord/GitHub natively) | One `ClientRegistration` + claim mapper per provider |
| Token model          | Keycloak issues JWTs, we validate via JWKS          | We issue our own session JWT after OAuth callback   |
| Lock-in              | Keycloak schema, admin API                          | Standard Spring Security; providers are swappable   |

Spring Security supports N providers out of the box; adding GitHub or Microsoft is a small follow-up
once Google is in place. Keycloak's federation advantage is real but the per-provider overhead in
Spring is low, and the ops cost of running a Keycloak instance is not.

## Scope

- **Phase 1 — Google sign-in + persistent accounts** (Postgres-backed, schema `auth`).
- **Phase 2 — Per-account stats tracking** (Postgres schema `stats`): one row per completed game
  per authenticated participant, with simple aggregation endpoints. Guests don't record stats.
- **Schema and abstractions are provider-agnostic from day one.** Each provider is identified by a
  `(provider, subject)` tuple; the JIT-provisioning flow goes through a `ProviderClaimMapper`
  abstraction so adding GitHub / Microsoft is purely additive (no migrations, no rewiring).
- **Each provider gets a separate account.** Signing in with Google produces one
  `auth.user_accounts` row; later signing in with GitHub as the same human produces a second row.
  See [Open Questions](#open-questions--risks) for the rationale and the linking-as-follow-up note.
- **Session tokens live in `localStorage` for V1.** The browser holds a short-lived access JWT
  (`localStorage["argentum-session"]`, 1h) plus an opaque refresh token
  (`localStorage["argentum-refresh"]`, 30-day sliding). Both are delivered to the SPA via a URL
  fragment on the OAuth callback redirect. The access JWT is attached to REST as
  `Authorization: Bearer <jwt>` and to the WebSocket via the existing
  `ClientMessage.Connect.token` field; the refresh token only ever hits `POST /auth/refresh`.
  **No browser cookies.** Cookies are tracked as a hardening follow-up; see Security Notes.
- **Decks stay on the client.** Migrating the deck library (`localStorage["argentum.decks"]`) to
  a server-side store keyed by `userAccountId` is **deferred to a dedicated deck-library backlog
  item**, which will define both the server schema and the one-shot upload-on-first-login flow.

## Dependencies

This feature depends on:
- **Postgres** (Phase 1.1 below) — first relational store in the project; everything user-keyed
  flows from here.
- **PlayerIdentity state machine formalization**
  ([architecture #6](archived/game-server-architecture.md#6-formalize-playeridentity-state-machine)) —
  the auth flow needs a cleaner identity lifecycle than the current ad-hoc model.

This feature is a prerequisite for any feature that needs durable cross-device identity (player
profiles, deck library, match history, leagues, etc.).

---

## Persistence approach — Postgres vs Redis

The codebase today persists everything (game sessions, lobbies, tournament state) in **Redis with a
24-hour TTL**, via the pattern `interface XRepository` + `RedisXRepository` + `InMemoryXRepository`
gated by `cache.redis.enabled` (see `RedisGameRepository`, `RedisLobbyRepository`, and their
in-memory counterparts). **No durable, no-TTL data exists yet** — auth accounts and (Phase 2) stats
rows would be the first.

Two plausible approaches:

| Concern              | Postgres + JPA (this plan)                                       | Redis-backed `UserAccountRepository`                                   |
|----------------------|------------------------------------------------------------------|------------------------------------------------------------------------|
| New infra            | Postgres + Flyway                                                | None — Redis is already deployed                                       |
| Convention fit       | First JPA + first relational store in the project                | Matches the existing `RedisXRepository` + `InMemoryXRepository` shape  |
| Uniqueness           | DB-enforced `UNIQUE(provider, subject)`                          | App-enforced via a secondary-index key + MULTI/WATCH                   |
| Future queries       | SQL — admin lists, signup metrics, joins to future tables        | SCAN, or hand-rolled indexes per query                                 |
| TTL story            | No TTL by default — straightforward                              | First-ever non-expiring keys; ops must be careful with FLUSHDB/backups |
| Migrations           | Flyway-versioned SQL                                             | One-off scripts                                                        |
| Serializer           | JPA-managed entities                                             | `kotlinx.serialization` (matches the rest of the codebase)             |
| Audit posture        | Standard                                                         | Auditors / future-you side-eye user accounts in Redis                  |

**We choose Postgres.** Auth is the first piece of a relational backbone the project will need
anyway (profiles, deck library, match history, leagues). Paying the infra cost once now is cheaper
than running auth on Redis and migrating later, and SQL constraints (`UNIQUE`, foreign keys for
future tables — including the Phase 2 stats table and the eventual deck table) are exactly the
integrity guarantees we want for user data.

Redis-backed auth stays a sensible Plan B if this feature ships alone for longer than expected and
we'd rather defer the "first relational store" decision until a second persistent feature lands —
the `UserAccountRepository` interface in Phase 1.3 keeps that swap localized.

---

## Phase 1 — Accounts & Sign-in

### 1.1 Postgres Setup

- Add Postgres service to `docker-compose.local.yml` (and prod `docker-compose.yml`) alongside Redis.
- Dev defaults: `postgres:16-alpine`, port `5432`, db `argentum`, user `argentum`, password from env.
- **One shared database, per-feature Postgres schemas (namespaces).** Auth tables live in an `auth`
  schema; the Phase 2 stats tables live in a `stats` schema. Future features get their own schema.
  Keeps related tables grouped, makes migrations per-feature, and lets us grant scoped DB roles
  later without splitting databases.
- Spring Boot config (`application.yml`):
  ```yaml
  spring:
    datasource:
      url: ${DB_URL:jdbc:postgresql://localhost:5432/argentum}
      username: ${DB_USER:argentum}
      password: ${DB_PASSWORD:argentum}
    jpa:
      hibernate.ddl-auto: validate   # migrations own the schema
      open-in-view: false
  ```
- Redis stays as the game-state cache; Postgres owns durable, relational data.

### 1.2 Migrations (Flyway)

- Add `flyway-core` + `flyway-database-postgresql` to `game-server/build.gradle.kts`.
- Migration directory: `game-server/src/main/resources/db/migration/`.
- `V1__auth_schema.sql` creates the `auth` schema and `auth.user_accounts` (Phase 1.4).
- `V2__stats_schema.sql` creates the `stats` schema and `stats.game_results` (Phase 2.1).
- Future migrations live as further `V*__*.sql` files in the same directory, each creating and
  populating its own Postgres schema for the feature it owns.

### 1.3 Data Access Layer

Use **Spring Data JPA**. The codebase has no existing relational data-access layer to match, so the
choice is between JPA, Exposed, jOOQ, and raw JDBC. JPA is the most familiar option in Spring-land
and the simplest fit for the small set of tables in this feature. Wrap it behind
`UserAccountRepository` / `GameResultRepository` interfaces so the Postgres ↔ Redis decision in the
Persistence section above stays reversible.

Add to `game-server/build.gradle.kts`:
```kotlin
implementation("org.springframework.boot:spring-boot-starter-data-jpa")
implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
implementation("org.springframework.boot:spring-boot-starter-security")
runtimeOnly("org.postgresql:postgresql")
implementation("org.flywaydb:flyway-core")
runtimeOnly("org.flywaydb:flyway-database-postgresql")
// Session JWTs use Spring Security's built-in Nimbus JOSE+JWT support
// (already transitive via spring-boot-starter-oauth2-resource-server) — no extra JWT library.
```

### 1.4 UserAccount Entity

```kotlin
@Entity
@Table(
    name = "user_accounts",
    schema = "auth",
    uniqueConstraints = [UniqueConstraint(columnNames = ["provider", "subject"])]
)
class UserAccount(
    @Id val id: UUID,                            // our own UUID, generated on JIT provision
    @Column(nullable = false)
    val provider: String,                        // 'google' (Phase 1), 'github', 'microsoft', ...
    @Column(nullable = false)
    val subject: String,                         // provider's stable per-user ID (Google sub, GitHub id, MS oid)
    @Column(nullable = false)
    var email: String,                           // provider's email at last login (may change over time)
    @Column(nullable = false)
    var displayName: String,                     // editable by user; defaults to provider's name
    var avatarUrl: String?,                      // provider's avatar (may be null, e.g. MS personal)
    val createdAt: Instant,
    var lastLoginAt: Instant,
    @Enumerated(EnumType.STRING)
    var status: AccountStatus = AccountStatus.ACTIVE   // ACTIVE, DELETED  (SUSPENDED is a future addition)
)
```

Initial migration (`V1__auth_schema.sql`):
```sql
CREATE SCHEMA IF NOT EXISTS auth;

CREATE TABLE auth.user_accounts (
    id              UUID PRIMARY KEY,
    provider        VARCHAR(32)  NOT NULL,
    subject         VARCHAR(128) NOT NULL,
    email           VARCHAR(320) NOT NULL,
    display_name    VARCHAR(80)  NOT NULL,
    avatar_url      TEXT,
    created_at      TIMESTAMPTZ  NOT NULL,
    last_login_at   TIMESTAMPTZ  NOT NULL,
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    UNIQUE (provider, subject)
);
CREATE INDEX idx_user_accounts_email ON auth.user_accounts (lower(email));
```

`UserAccountRepository : JpaRepository<UserAccount, UUID>` exposes
`findByProviderAndSubject(provider: String, subject: String)`.

### 1.5 OAuth2 Login Flow (Spring Security)

Configure two Spring Security filter chains:

1. **OAuth2 login chain** — `/oauth2/**` and `/login/oauth2/**` handle the browser-initiated flow
   for any registered provider.
2. **API/WebSocket chain** — `/api/**` and `/game` validate our own session JWT (see 1.6).

`SecurityConfig.kt`:
```kotlin
@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val accountProvisioner: AccountProvisioner,
    private val sessionTokenService: SessionTokenService
) {
    @Bean @Order(1)
    fun authChain(http: HttpSecurity): SecurityFilterChain = http
        .securityMatcher("/oauth2/**", "/login/oauth2/**", "/auth/**")
        .oauth2Login { o -> o
            .successHandler(::onLoginSuccess)
            .failureHandler(::onLoginFailure)
        }
        .csrf { it.disable() }      // SPA — no session cookies; bearer tokens are CSRF-safe
        .build()

    @Bean @Order(2)
    fun apiChain(http: HttpSecurity): SecurityFilterChain = http
        .securityMatcher("/api/**", "/game")
        .authorizeHttpRequests { a -> a
            .requestMatchers("/api/admin/**").hasRole("ADMIN")
            .requestMatchers("/api/dev/**").permitAll()      // E2E scenarios — gated by config
            .anyRequest().permitAll()                        // WebSocket auth happens in-message
        }
        .oauth2ResourceServer { it.jwt { jwt -> jwt.decoder(sessionJwtDecoder()) } }
        .sessionManagement { it.sessionCreationPolicy(STATELESS) }
        .csrf { it.disable() }
        .build()
}
```

Spring `application.yml` — Phase 1 wires only Google; the `github:` / `microsoft:` blocks slot in
later without other changes:
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_OAUTH_CLIENT_ID:}
            client-secret: ${GOOGLE_OAUTH_CLIENT_SECRET:}
            scope: openid, profile, email
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
          # github:        # add when wiring GitHub login
          #   client-id: ${GITHUB_OAUTH_CLIENT_ID:}
          #   client-secret: ${GITHUB_OAUTH_CLIENT_SECRET:}
          #   scope: read:user, user:email
          # microsoft:     # add when wiring Microsoft login
          #   client-id: ${MS_OAUTH_CLIENT_ID:}
          #   client-secret: ${MS_OAUTH_CLIENT_SECRET:}
          #   scope: openid, profile, email
          #   provider: microsoft
        # provider:
        #   microsoft:
        #     issuer-uri: https://login.microsoftonline.com/common/v2.0
```

### 1.6 JIT Provisioning & Session Token Issuance

```
1. Spring's oauth2Login filter validates the provider's response and gives us an OAuth2User /
   OidcUser plus the OAuth2AuthenticationToken (carries `registrationId`, e.g. "google").

2. ProviderClaimMapper.map(registrationId, oauth2User) -> ProviderIdentity(
       provider:     registrationId,
       subject:      provider's stable user ID,
       email:        primary verified email (may require a follow-up call, see Provider Notes),
       displayName:  provider's name (fallback: provider's login/handle),
       avatarUrl:    provider's picture/avatar URL (nullable)
   )
   Phase 1 registers only a Google mapper. The interface is the only thing GitHub/Microsoft need
   to implement when they land.

3. AccountProvisioner.findOrCreate(identity):
     - lookup auth.user_accounts WHERE provider = identity.provider AND subject = identity.subject
     - if missing: insert new row (id = UUID.randomUUID(), createdAt = now)
     - if present: update email/displayName/avatarUrl from latest claims, set lastLoginAt = now

4. SessionTokenService.issuePair(userAccount):
     - **Access JWT** (short-lived, ~1h):
       - sign via Spring Security's JwtEncoder (Nimbus, transitively present)
       - HS256 with a server-side secret (env `GAME_SESSION_SECRET`); easy to swap to RS256/JWKS later
       - claims: sub = userAccount.id, name = displayName, prv = userAccount.provider,
                 jti = UUID.randomUUID() (used by the revocation list — see Security Notes),
                 exp = now+1h, iss = "argentum"
       - matching JwtDecoder bean (sessionJwtDecoder) is wired into the API/WebSocket filter chain.
     - **Refresh token** (long-lived, opaque):
       - 256-bit random, URL-safe encoded.
       - stored in Redis at `auth:refresh:{token}` as JSON
         `{ userAccountId, family: UUID, issuedAt: Instant }` with **30-day sliding TTL**
         (reset on each successful use).
       - `family` is a UUID shared across every refresh in the rotation chain; used for theft
         detection (see refresh flow below).
       - also indexed at `auth:refresh-by-account:{userAccountId}` (SADD the token) so we can
         revoke all of a user's sessions in one shot.

5. Redirect the browser to the SPA with both tokens in a URL fragment:
       302 -> https://app/#auth_token=<jwt>&refresh_token=<refresh>
   Fragment (not query string) so neither token is sent on the redirect request or logged in
   access logs. The SPA reads `window.location.hash`, persists the access JWT to
   `localStorage["argentum-session"]` and the refresh token to `localStorage["argentum-refresh"]`,
   then clears the fragment via `history.replaceState` so the tokens don't linger in browser
   history or get accidentally copy-pasted.
```

We issue **our own tokens** rather than passing the provider's `id_token`/access token to the SPA:
- The provider token carries PII and provider-specific claims we don't want on the client; our
  access JWT only carries `userAccountId`, `displayName`, and `provider`.
- We control rotation and revocation (Redis `jti` blocklist + refresh-token family — Security Notes).
- We don't store **provider** refresh tokens; we issue our own server-side refresh tokens instead.
  When our refresh token's sliding 30-day TTL runs out, the user re-authenticates with the provider
  (one click).

**Token refresh flow** (`POST /auth/refresh`, body `{ refreshToken: "..." }`):

1. Look up `auth:refresh:{token}` in Redis.
   - **Missing** → check `auth:refresh-tombstone:{token}`. If a tombstone exists, see step 3
     (reuse). Otherwise → 401, client clears both local tokens and drops to the guest flow.
   - **Found** → continue to step 2.
2. **Rotate**: delete the old refresh token, write a tombstone
   `auth:refresh-tombstone:{token} = family` with the family's remaining TTL, issue a fresh access
   JWT and a new refresh token **in the same `family`**, update the per-account index. Return
   `{ accessToken, refreshToken, expiresIn: 3600 }`.
3. **Family-reuse detection.** A presented token that's missing but tombstoned was rotated away —
   the legitimate client should already be carrying the new token, so reuse means theft. Revoke
   **every refresh token in that family** (delete all members of
   `auth:refresh-by-account:{userAccountId}` whose stored `family` matches) and force re-auth on
   every device. Also blocklist any currently outstanding access JWT in that family by `jti`.

Sliding TTL means a user who visits at least monthly stays logged in indefinitely; one who doesn't
gets bounced back to the OAuth flow.

### 1.7 WebSocket Authentication

**The WebSocket flow stays the same as today** — auth happens on the first
`ClientMessage.Connect(playerName, token)`. The only change: `token` can now be a signed JWT
(authenticated path) instead of a random UUID (legacy guest path). The server tries to decode and
validate; if the JWT is valid, the connection is authenticated, otherwise it falls back to the
existing UUID-token reconnect or fresh-guest paths.

`ConnectionHandler.handleConnect()` becomes:
```kotlin
val accountId: UUID? = message.token?.let(sessionTokenService::tryDecodeAccountId)
when {
    accountId != null     -> handleAuthenticatedConnect(session, accountId, message)
    message.token != null -> handleLegacyTokenReconnect(session, message)   // existing UUID path
    else                  -> handleGuestConnect(session, message)            // fresh guest
}
```

`PlayerIdentity` gains a nullable account link (keeping all its existing fields —
`playerId`, `playerName`, `isAi`, `aiModelOverride`, `webSocketSession`, `currentGameSessionId`,
`currentLobbyId`, `currentQuickGameLobbyId`, `currentSpectatingGameId`, the two disconnect timers):
```kotlin
class PlayerIdentity(
    val token: String = UUID.randomUUID().toString(),
    val userAccountId: UUID? = null,        // null for guests
    /* ...existing fields unchanged... */
)
```
`SessionRegistry` gains a secondary index `identityByAccountId: ConcurrentHashMap<UUID, PlayerIdentity>`
alongside the existing `playerIdentities` and `wsToToken` maps, so an authenticated reconnect from a
different browser still resolves to the same identity once the new device has its own JWT.

Reconnect resolution order in `handleConnect()`:
1. JWT decodes successfully → look up identity by `userAccountId` (cross-device works).
2. Else `token` matches an existing guest UUID → existing token-based reconnect.
3. Else → fresh guest identity.

If the WebSocket close is triggered server-side by an expired JWT (or the server rejects a
`Connect` carrying an expired one), the SPA hits `/auth/refresh`, then reconnects with the new
JWT. See 1.11.

No `HandshakeInterceptor`, no cookies, no CORS-with-credentials. The existing
`setAllowedOrigins("*")` stays as-is for V1 — bearer tokens in a custom message field aren't
subject to the cookie auto-attachment risk that wildcard CORS otherwise enables. (Origin tightening
becomes necessary when we migrate to cookies — see Security Notes.)

### 1.8 Guest Mode

Anonymous play continues to work and is explicitly supported:

- WebSocket upgrade works as today; no JWT required.
- The client sends `ClientMessage.Connect(playerName, token=…)` with whatever it has — JWT for
  authenticated users, the legacy `argentum-token` UUID for guests, or no token for fresh sessions.
- `PlayerIdentity.userAccountId` is null for guests; `SessionRegistry` still indexes guests by `token`.
- Server-issued UUID token is returned in `ServerMessage.Connected` and stored in
  `localStorage["argentum-token"]` (the existing key, untouched by this feature).
- Account-scoped REST endpoints (`/api/me`, `/api/me/stats`, `/api/me/games`) **reject**
  unauthenticated requests (`401`); gameplay endpoints (lobby create, quick-game, dev scenarios)
  remain open.

This preserves:
- Drop-in casual play (no account barrier).
- Existing E2E scenario flows in `e2e-scenarios/` that rely on `?token=` URL params and pre-registered
  identities via `DevScenarioController.preRegisterIdentity()` / `SessionRegistry.preRegisterIdentity()`.
- The `X-Admin-Password` flow on `/api/admin/**` continues to work in parallel for now (see 1.10
  for the admin migration plan).

### 1.9 REST Endpoints

Spring's defaults handle the OAuth dance per registration; the per-provider URLs are derived from
`registrationId`. All `/api/**` endpoints require `Authorization: Bearer <jwt>` (the API chain's
`oauth2ResourceServer.jwt(...)` rejects anonymous calls except those explicitly `permitAll`).

```
GET    /oauth2/authorization/google     Initiates Google OAuth2 (Spring redirects)
GET    /login/oauth2/code/google        Callback (Spring handles, we run successHandler)
                                        (mirror routes appear automatically for /github, /microsoft)
POST   /auth/refresh                    Exchange refresh token for a new access+refresh pair
                                        (body: { refreshToken }; see 1.6 refresh flow)
POST   /auth/logout                     Revokes the current access JWT (jti blocklist) AND every
                                        refresh token in its family; client clears both localStorage
                                        keys on success.
GET    /api/me                          Returns the current UserAccount (or 401 if no/invalid JWT)
PATCH  /api/me                          Update displayName (only mutable field by user)
DELETE /api/me                          Soft-delete: status=DELETED, anonymize email/displayName,
                                        cascade-delete stats rows (see Phase 2)
```

`/api/me` shape:
```json
{
  "id": "uuid",
  "provider": "google",
  "displayName": "string",
  "email": "string",
  "avatarUrl": "string|null",
  "createdAt": "iso8601"
}
```

### 1.10 Roles & Admin Migration

- Add `auth.user_accounts.roles VARCHAR[]` (Postgres array) or an `auth.user_roles` join table for `ADMIN`.
- The `X-Admin-Password` check in `AdminController.checkAuth()` is preserved as a fallback during
  rollout but is deprecated; admin access also accepts a session JWT with the `ADMIN` role
  (`Authorization: Bearer <jwt>`).
- A bootstrap admin can be promoted via a one-time SQL migration referencing their
  `(provider, subject)`, or via env-configured allowlists (e.g. `GAME_ADMIN_GOOGLE_EMAILS`) checked
  at JIT-provision time.
- Schedule removal of the `X-Admin-Password` header as a separate cleanup item once at least one
  admin account exists and the admin UI sends the session JWT.

### 1.11 Frontend Integration

`web-client` changes (`src/store/slices/connectionSlice.ts`, `connectionHandlers.ts`,
`src/components/landing/`, plus a new `authSlice`):

- **OAuth callback bootstrap**: on app load, the root effect checks `window.location.hash` for
  `#auth_token=<jwt>&refresh_token=<refresh>`. If present:
  1. Write the JWT to `localStorage["argentum-session"]` and the refresh token to
     `localStorage["argentum-refresh"]`.
  2. `history.replaceState(null, '', window.location.pathname + window.location.search)` to scrub.
- **Login button(s)**: Phase 1 shows "Sign in with Google" →
  `window.location.href = '/oauth2/authorization/google'`. The button list is data-driven so adding
  GitHub / Microsoft is a one-line change.
- **Session detection**: on boot, if a JWT exists in localStorage, `GET /api/me` with
  `Authorization: Bearer <jwt>` → store user in `authSlice`. 401 → fall through to the refresh
  flow below.
- **Auto-refresh on 401** (REST): a shared `apiFetch()` wrapper retries once. On `401`, call
  `POST /auth/refresh` with the stored refresh token; on success, persist the new pair and retry
  the original request. On refresh failure (401 from `/auth/refresh`), clear both localStorage
  keys + `authSlice` and demote to guest. **Single retry only** — no infinite loops.
- **WebSocket connect**: include the current access JWT (if present) as the `token` field in
  `ClientMessage.Connect`. Guests keep using `localStorage["argentum-token"]`. The dev-scenario
  `?token=` URL param keeps working as it does today (server side decides whether the value is a
  JWT or a guest UUID).
- **WebSocket expiry**: if the server rejects `Connect` with a token-expired reason (or closes the
  WS with the same), the SPA runs the refresh flow once and reconnects. Same single-retry rule.
- **Display name**: pre-fill the player-name input with `me.displayName` for authenticated users
  (still editable for the session — open question whether we want per-game name override now that
  accounts exist).
- **Logout**: `POST /auth/logout` (with the access JWT) → clear both localStorage keys and the
  `authSlice` → close + reopen the WebSocket so it reconnects as a guest.
- **Reverse proxy**: in dev, the Vite proxy must cover `/oauth2/**` and `/login/oauth2/**` so the
  redirect dance works from `localhost:5173`. No cookie forwarding needed. The Google OAuth client
  must whitelist `http://localhost:5173/login/oauth2/code/google` and the prod equivalent.

---

## Phase 2 — Player Stats

Persist per-account game outcomes so logged-in users can see their record over time. Append-only:
each completed game inserts one row per authenticated participant. Guests don't track stats.

### 2.1 `stats.game_results` Schema

```sql
CREATE SCHEMA IF NOT EXISTS stats;

CREATE TABLE stats.game_results (
    id                UUID PRIMARY KEY,
    user_account_id   UUID NOT NULL REFERENCES auth.user_accounts(id) ON DELETE CASCADE,
    game_session_id   VARCHAR(64) NOT NULL,         -- the existing in-memory game session id
    outcome           VARCHAR(16) NOT NULL,         -- WON, LOST, DRAW, CONCEDED
    format            VARCHAR(32),                  -- constructed, sealed, draft, commander, ...
    opponents_count   SMALLINT NOT NULL,
    duration_seconds  INTEGER,
    played_at         TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_game_results_user_played_at
    ON stats.game_results (user_account_id, played_at DESC);
```

Notes:
- **Append-only, no aggregation table for now.** Aggregations (`games_played`, `win_rate`, etc.)
  are computed at read time via SQL `GROUP BY`. With current player counts this is well under a
  millisecond; we can add a materialized view later if it stops being free.
- **`game_session_id` is `VARCHAR(64)` and not a foreign key** — game sessions live in Redis with
  a 24h TTL, so there's no durable referent for an FK.
- **`outcome` enum** — `WON`, `LOST`, `DRAW`, `CONCEDED`. Concedes are tracked separately from a
  clean loss because the future UI may want to surface "completion rate" alongside "win rate".
- **`ON DELETE CASCADE`** on the account FK so soft-deleting an account also drops its stats rows.
  (`DELETE /api/me` in Phase 1.9 mentions this cascade.)

### 2.2 Entity & Repository

```kotlin
@Entity
@Table(name = "game_results", schema = "stats")
class GameResult(
    @Id val id: UUID,
    @Column(nullable = false) val userAccountId: UUID,
    @Column(nullable = false) val gameSessionId: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false) val outcome: GameOutcome,   // WON, LOST, DRAW, CONCEDED
    val format: String?,
    @Column(nullable = false) val opponentsCount: Short,
    val durationSeconds: Int?,
    @Column(nullable = false) val playedAt: Instant
)

enum class GameOutcome { WON, LOST, DRAW, CONCEDED }
```

`GameResultRepository : JpaRepository<GameResult, UUID>` with:
- `findAllByUserAccountIdOrderByPlayedAtDesc(userId: UUID, pageable: Pageable): Page<GameResult>`
- a `@Query` aggregation projection used by `/api/me/stats` — `count(*) FILTER (WHERE outcome = ?)`
  grouped per outcome, plus `max(played_at)`.

### 2.3 Recording Game Outcomes

A new `StatsRecorder` listens for game-completion and writes one `GameResult` row per
authenticated participant. The hook sits next to the existing game-end handling in `game-server`
(wherever the engine signals "game over"). For each player in the final state:

- If `PlayerIdentity.userAccountId != null` → insert a `GameResult` row.
- Otherwise → skip (guests don't track stats).

The write is **best-effort and asynchronous** — it doesn't gate game-end notifications to clients,
and a failure logs an error but doesn't roll back the game outcome on the client. Concedes vs
clean wins are distinguished by reading the existing end-state reason; the engine already produces
this distinction for spectator UI.

Format is read from the lobby's configured format (where present); null otherwise. Duration is
`endedAt - startedAt` from the existing game-session metadata.

### 2.4 REST Endpoints

```
GET    /api/me/stats                    Aggregated stats for the current user
GET    /api/me/games                    Recent game history (paginated)
```

`GET /api/me/stats`:
```json
{
  "gamesPlayed": 42,
  "gamesWon": 23,
  "gamesLost": 16,
  "gamesDrawn": 1,
  "gamesConceded": 2,
  "lastPlayedAt": "iso8601|null"
}
```

`GET /api/me/games?cursor=…` (default page size 20):
```json
{
  "items": [
    {
      "id": "uuid",
      "outcome": "WON",
      "format": "constructed",
      "opponentsCount": 1,
      "durationSeconds": 1340,
      "playedAt": "iso8601"
    }
  ],
  "nextCursor": "string|null"
}
```

Both endpoints require a valid bearer JWT (the API chain rejects anonymous). Pagination uses an
opaque cursor (base64-encoded `(playedAt, id)`) rather than offsets so new games landing during
pagination don't shift the window.

### 2.5 Frontend (light)

Phase 2 ships the data plumbing only — no stats UI. A profile badge or sidebar section showing
win/loss can land in a follow-up; the endpoints above unblock that work without adding any new
WebSocket plumbing.

---

## Provider Notes

Each new provider needs three small additions: a `ClientRegistration` in `application.yml`, a
`ProviderClaimMapper` implementation, and a button on the frontend. The quirks below are the only
non-obvious bits.

- **Google (Phase 1).** OIDC standard. Claims used: `sub` → `subject`, `email`, `name` →
  `displayName`, `picture` → `avatarUrl`. Spring auto-configures everything via
  `CommonOAuth2Provider.GOOGLE`.

- **GitHub (follow-up).** OAuth2, not OIDC. Userinfo from `GET https://api.github.com/user`. Map
  `id` (stringify the integer) → `subject`, `name ?? login` → `displayName`, `avatar_url` →
  `avatarUrl`. **Email may be null** if the user keeps it private — request the `user:email` scope
  and call `GET /user/emails` to find the primary verified address. Fallback for fully-private
  users: `{login}@users.noreply.github.com` (synthetic but stable).

- **Microsoft (follow-up).** OIDC via Microsoft identity platform / Entra ID. Use
  `issuer-uri: https://login.microsoftonline.com/common/v2.0` to accept both personal and
  work/school accounts. Subject: prefer `oid` (object ID, cross-tenant stable) and fall back to
  `sub` if absent. Email lives in `email` or `preferred_username`. **Profile picture is not in the
  ID token** — getting it requires a Microsoft Graph call (`GET /me/photo/$value`) with the
  `User.Read` scope; skip it on first cut and leave `avatarUrl` null.

---

## Technical Considerations

### Infrastructure Additions

| Component        | Purpose                                                                  | Dev Setup                       | Prod Setup                       |
|------------------|--------------------------------------------------------------------------|---------------------------------|----------------------------------|
| **Postgres**     | `auth.user_accounts`, `stats.game_results`, + future relational data     | Docker Compose                  | Managed Postgres                 |
| **Flyway**       | Schema migrations                                                        | Bundled in `bootRun`            | Bundled in deploy                |
| **OAuth IdPs**   | Identity providers (Google now; GitHub / Microsoft to follow)            | OAuth client secret(s) in `.env` | Same, in deploy secret store    |
| **Redis**        | Game cache (already present) + JWT revocation list + refresh-token store | Already configured              | Already configured               |

### Migration Strategy from Ephemeral PlayerIdentity

The current ephemeral `PlayerIdentity` (random UUID in `localStorage`) must coexist during transition:

1. **Phase 1 ships both paths**: JWT-bearing connections become authenticated identities;
   token-less or UUID-token connections remain guest identities. No data migration is needed in
   Phase 1 because there's no server-side per-user data yet.
2. **Guest → Account merge**: when a guest player logs in, we have an open question (see Risks)
   about whether to migrate the current `PlayerIdentity.token` mapping to the new
   `userAccountId`-keyed identity, or to start a fresh authenticated identity and let the in-flight
   game finish under the guest token. Recommended default: **start a fresh authenticated identity
   on next connect; let the in-flight guest session complete naturally**.
3. **Pre-account games stay anonymous.** Phase 2 only records stats for games completed *after*
   the player's account exists; there's no backfill path for past anonymous play.
4. **Client localStorage data is left in place.** Per-device data already exists today: saved
   decks at `localStorage["argentum.decks"]` (`web-client/src/store/deckLibrary.ts`), preferences
   at `argentum-auto-tap` / `argentum-stop-overrides`, plus `argentum-player-name`,
   `argentum-token`, `argentum-lobby-id`, `argentum-bg`. This feature does **not** touch any of it
   on first sign-in; decks and preferences remain per-device. Migrating the deck library to a
   server-side store (keyed by `userAccountId`) is **deferred to a dedicated deck-library backlog
   item**, which will define both the server schema and the one-shot upload-on-first-login flow.
5. **E2E scenarios** keep using the existing `?token=` + `DevScenarioController.preRegisterIdentity()`
   path verbatim; that is the guest path. No test rewrites required.

### Security Notes

- **Session JWT signing key**: HMAC secret in env (`GAME_SESSION_SECRET`), rotated by writing a new
  secret and accepting both old + new during a grace window. (Or RS256 with a JWKS endpoint if we
  want zero-downtime rotation built in.)
- **Tokens in `localStorage` are XSS-exposed.** Any script running on our origin can read both the
  access JWT and the refresh token. We accept this risk for V1 because:
  - The product surface today is small (no payments, no third-party scripts).
  - React with the existing render patterns (no `dangerouslySetInnerHTML` on user input) gives
    decent baseline protection.
  - Access JWT lifetime is capped at 1h — a stolen access token gives an attacker at most an hour
    of activity (or until `/auth/logout` blocklists the `jti`).
  - Refresh tokens **rotate on every use**; reuse of a rotated-out token triggers family-wide
    revocation, which means a stealthy attacker still trips a tripwire the moment the legitimate
    client refreshes (or vice-versa). Worst case is a stolen refresh token used silently until the
    legitimate client next refreshes — typically minutes to hours.
  Tracked follow-up: migrate to `HttpOnly`, `Secure`, `SameSite=Lax` cookies. That requires
  tightening WebSocket origins (today `setAllowedOrigins("*")` — incompatible with cookie-bearing
  upgrades), exposing a `HandshakeInterceptor` to read the cookie at upgrade, and adding a
  `Set-Cookie` step to the success handler. Worth the work when we add anything sensitive
  (payments, real-time chat history, etc.).
- **Refresh tokens** are opaque, stored only in Redis (never in the JWT signing material), rotated
  on each `/auth/refresh`, and family-tagged so a single theft event invalidates the whole chain.
  Per-account index (`auth:refresh-by-account:{userAccountId}`) supports global "sign out
  everywhere" without scanning.
- **CSRF**: not applicable in V1. Bearer tokens in `Authorization` headers don't auto-attach
  cross-origin and aren't sent on form posts. When we eventually move to cookies, revisit.
- **Open redirect**: the success handler must validate any post-login redirect target against an
  allowlist of own-origin paths.
- **Account takeover via email is not a thing here.** Accounts are keyed by `(provider, subject)`,
  never by email. A new sign-in is **never auto-linked** to an existing account by matching email
  — that would let a hostile IdP take over an account by claiming a victim's address. Different
  providers → different accounts. If account-linking is added later it must require the user to be
  authenticated with the first identity first.
- **Rate limiting**: `/oauth2/authorization/*`, `POST /auth/refresh`, and `PATCH /api/me` — basic
  per-IP throttling via a Spring filter. The refresh endpoint is the most attractive brute-force
  target so it should be the tightest.
- **Logout revocation**: blocklist the access JWT's `jti` in Redis with TTL = remaining lifetime
  AND delete every refresh token in the same family. The resource-server JWT decoder consults the
  blocklist on each request; the refresh endpoint already checks family membership.

### Reconnection Semantics

| Scenario                                            | Outcome                                                                                                                                                                                  |
|-----------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Authenticated reconnect, same browser, JWT fresh    | JWT in `localStorage["argentum-session"]` → included in `ClientMessage.Connect.token` → decoded → `PlayerIdentity` found via `identityByAccountId` → in-flight game/lobby restored.       |
| Authenticated reconnect, JWT expired                | Connect rejected (or server closes WS) → SPA hits `/auth/refresh`, stores the new pair, reconnects with the new JWT. Silent to the user.                                                  |
| Authenticated reconnect, different browser/device   | The new device has no JWT until the user logs in there too. After login it has its own JWT pointing at the same `userAccountId`, so the server-side `PlayerIdentity` is resumed.          |
| Refresh token also expired (>30d inactive)          | `/auth/refresh` returns 401 → SPA clears tokens → user is back to the landing page guest flow → must click "Sign in with Google" again.                                                   |
| Guest reconnect                                     | `localStorage["argentum-token"]` (legacy UUID) → existing token-based reconnect, unchanged from today.                                                                                    |
| Authenticated user clears `localStorage` mid-game   | Next `Connect` has no token → treated as guest → cannot resume; game auto-concedes after the existing grace period.                                                                       |
| Guest logs in mid-game                              | See Migration Strategy #2 — current game finishes as guest, next session is authenticated.                                                                                                |

### Scaling Notes

- `auth.user_accounts` is tiny and read-heavy. The `(provider, subject)` unique constraint and
  `lower(email)` index are sufficient indefinitely.
- `stats.game_results` grows linearly with games × authenticated participants. The composite index
  on `(user_account_id, played_at DESC)` covers both `/api/me/stats` and `/api/me/games`. Rows are
  small; we can keep history indefinitely until volume becomes a reason to partition by month.
- JWT validation is local (no DB hit per request) — only `/auth/refresh`, logout, and admin-role
  checks hit Postgres / Redis; every request consults the Redis `jti` blocklist (single GET,
  sub-ms).
- Postgres connection pool size: 10 is plenty for auth + stats workload.

---

## Open Questions / Risks

1. **Guest → account merge UX.** If a guest is mid-game when they hit "Sign in with …", do we
   (a) defer login until game-over, (b) silently swap the underlying identity (risky — `EntityId`
   for the player is engine-internal and may be referenced from `GameState`), or (c) finish the
   guest game and start the next session authenticated? Default proposed: (c). Decide before
   shipping the login buttons.

2. **Same human across providers = separate accounts.** Signing in with Google then later with
   GitHub creates two `auth.user_accounts` rows. Trade-off: a simpler, safer model than
   email-based auto-linking (which is exploitable — see Security Notes). If we later want a single
   account with multiple linked sign-ins, it's a follow-up feature: an authenticated user adds
   another identity, which inserts a row into a separate `auth.linked_identities` table; the
   schema added in Phase 1 stays compatible.

3. **Display-name uniqueness.** Multiple accounts can share a display name (especially across
   providers). Do we enforce uniqueness? Recommendation: **no** — display names are not
   identifiers, the UUID is. We can show a `#1234` suffix or provider badge in disambiguating
   contexts.

4. **Email change handling.** If the provider's primary email changes between logins, we update
   the row in JIT provisioning. This is silent; surface it in `/api/me` so the user sees it on
   next view.

5. **Admin migration cutover.** Until we promote at least one admin to a `UserAccount` with the
   `ADMIN` role, the `X-Admin-Password` header is the only admin path. Don't remove the header
   check in Phase 1 — schedule its removal as a separate cleanup item once an admin account exists
   and the admin UI sends the session JWT.

6. **Multiple browser tabs.** Each tab opens its own WebSocket. Today this works because identities
   are token-keyed. With account-keyed auth, all tabs share an account; the `SessionRegistry`
   needs to decide whether to allow multiple concurrent WebSockets per `userAccountId` (probably
   yes — spectating one game while in another lobby) or enforce single-session (kick the older
   socket). Recommendation: allow multiple, but route `GameSession` participation through the
   `currentGameSessionId` field on `PlayerIdentity` so only one tab is the player.

7. **`AccountStatus.SUSPENDED`.** Listed in the enum for future use but no V1 trigger
   (admin action vs auto-abuse-detection). Drop the value from V1 and reintroduce when there's a
   real use case, or keep it as a placeholder. Currently leaning toward **drop until needed.**

8. **GDPR right-to-be-forgotten vs soft-delete.** `DELETE /api/me` is currently soft-delete +
   anonymize, which is generally accepted as compliant when PII fields are nulled. If we ever
   need hard deletion (e.g. explicit right-to-erasure request that doesn't accept anonymization),
   add a separate admin path that cascades through `auth.user_accounts` and any FK-linked tables
   (`stats.game_results` already cascades).

9. **Stats for AI/bot opponents.** Should games against the AI count toward `games_played` and
   `gamesWon`? Default proposed: **yes, all completed games count** — simpler, and bot-vs-human
   is already tagged by `PlayerIdentity.isAi`, so we can filter at the UI level later if we want a
   "vs humans only" view. Decide before shipping the stats endpoints.

10. **Refresh-token horizon vs forced re-login policy.** 30-day sliding TTL is generous; some
    products bake in a hard maximum (e.g. 90 days absolute, regardless of activity) to force
    occasional re-auth. Keep the simple sliding model for V1; revisit if a security incident
    demands a hard ceiling.
