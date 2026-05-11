# OAuth2 Accounts & Persistent Identity

A direct OAuth2 / OIDC + Spring Security implementation that replaces the current ephemeral
`PlayerIdentity` (random UUID in `localStorage`) with persistent user accounts backed by Postgres,
and moves the client-side deck library to a server-side store keyed by the new account. **Google**
is the first wired-up provider; **GitHub** and **Microsoft** are designed-for follow-ups that drop
in via a new `ClientRegistration` and a per-provider claim mapper. Anonymous guest play stays
intact for casual games and dev scenario E2E tests.

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
- **Phase 2 — Deck library moves server-side** (Postgres schema `decks`, keyed by `userAccountId`),
  with a one-shot upload on first sign-in that imports any existing `localStorage["argentum.decks"]`
  entries.
- **Schema and abstractions are provider-agnostic from day one.** Each provider is identified by a
  `(provider, subject)` tuple; the JIT-provisioning flow goes through a `ProviderClaimMapper`
  abstraction so adding GitHub / Microsoft is purely additive (no migrations, no rewiring).
- **Each provider gets a separate account.** Signing in with Google produces one
  `auth.user_accounts` row; later signing in with GitHub as the same human produces a second row.
  See [Open Questions](#open-questions--risks) for the rationale and the linking-as-follow-up note.
- **Session token lives in `localStorage` for V1.** The JWT is delivered to the SPA via a URL
  fragment on the OAuth callback redirect, then stored at `localStorage["argentum-session"]` and
  attached to REST requests as `Authorization: Bearer <jwt>` and to the WebSocket via the existing
  `ClientMessage.Connect.token` field. **No browser cookies.** Cookies are tracked as a hardening
  follow-up; see Security Notes.

## Dependencies

This feature depends on:
- **Postgres** (Phase 1.1 below) — first relational store in the project; everything user-keyed
  flows from here.
- **PlayerIdentity state machine formalization**
  ([architecture #6](archived/game-server-architecture.md#6-formalize-playeridentity-state-machine)) —
  the auth flow needs a cleaner identity lifecycle than the current ad-hoc model.

This feature is a prerequisite for any feature that needs durable cross-device identity (player
profiles, match history, leagues, etc.).

---

## Persistence approach — Postgres vs Redis

The codebase today persists everything (game sessions, lobbies, tournament state) in **Redis with a
24-hour TTL**, via the pattern `interface XRepository` + `RedisXRepository` + `InMemoryXRepository`
gated by `cache.redis.enabled` (see `RedisGameRepository`, `RedisLobbyRepository`, and their
in-memory counterparts). **No durable, no-TTL data exists yet** — auth accounts and (Phase 2) decks
would be the first.

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
future tables — including the Phase 2 deck table) are exactly the integrity guarantees we want for
user data.

Redis-backed auth stays a sensible Plan B if this feature ships alone for longer than expected and
we'd rather defer the "first relational store" decision until a second persistent feature lands —
the `UserAccountRepository` interface in Phase 1.3 keeps that swap localized.

---

## Phase 1 — Accounts & Sign-in

### 1.1 Postgres Setup

- Add Postgres service to `docker-compose.local.yml` (and prod `docker-compose.yml`) alongside Redis.
- Dev defaults: `postgres:16-alpine`, port `5432`, db `argentum`, user `argentum`, password from env.
- **One shared database, per-feature Postgres schemas (namespaces).** Auth tables live in an `auth`
  schema; the Phase 2 deck tables live in a `decks` schema. Future features get their own schema.
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
- `V2__decks_schema.sql` creates the `decks` schema and `decks.decks` (Phase 2.1).
- Future migrations live as further `V*__*.sql` files in the same directory, each creating and
  populating its own Postgres schema for the feature it owns.

### 1.3 Data Access Layer

Use **Spring Data JPA**. The codebase has no existing relational data-access layer to match, so the
choice is between JPA, Exposed, jOOQ, and raw JDBC. JPA is the most familiar option in Spring-land
and the simplest fit for the small set of tables in this feature. Wrap it behind
`UserAccountRepository` / `DeckRepository` interfaces so the Postgres ↔ Redis decision in the
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

4. SessionTokenService.issue(userAccount):
     - sign a self-issued JWT via Spring Security's JwtEncoder (Nimbus, transitively present)
     - HS256 with a server-side secret (env `GAME_SESSION_SECRET`); easy to swap to RS256/JWKS later
     - claims: sub = userAccount.id, name = displayName, prv = userAccount.provider,
               jti = UUID.randomUUID() (used by the revocation list — see Security Notes),
               exp = now+12h, iss = "argentum"
     - matching JwtDecoder bean (sessionJwtDecoder) is wired into the API/WebSocket filter chain.

5. Redirect the browser to the SPA with the JWT in a URL fragment:
       302 -> https://app/#auth_token=<jwt>
   Fragment (not query string) so the JWT isn't sent on the redirect request and isn't logged in
   access logs. The SPA reads `window.location.hash`, persists the JWT to
   `localStorage["argentum-session"]`, and clears the fragment via `history.replaceState` so the
   token doesn't linger in browser history or get accidentally copy-pasted.
```

We issue **our own JWT** rather than passing the provider's `id_token`/access token to the SPA:
- The provider token carries PII and provider-specific claims we don't want on the client; our
  session JWT only carries `userAccountId`, `displayName`, and `provider`.
- We control rotation and revocation (Redis `jti` blocklist — Security Notes).
- Refresh is decoupled from the provider: we don't store provider refresh tokens. When the session
  JWT expires (12h) the user re-authenticates with one click.

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
- Account-scoped REST endpoints (`/api/me`, `/api/decks/**`) **reject** unauthenticated requests
  (`401`); gameplay endpoints (lobby create, quick-game, dev scenarios) remain open.

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
POST   /auth/logout                     Revokes the JWT via the Redis jti blocklist; client clears
                                        localStorage["argentum-session"] on success.
GET    /api/me                          Returns the current UserAccount (or 401 if no/invalid JWT)
PATCH  /api/me                          Update displayName (only mutable field by user)
DELETE /api/me                          Soft-delete: status=DELETED, anonymize email/displayName,
                                        cascade-delete decks (see Phase 2)
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

- **OAuth callback bootstrap**: on app load, `App.tsx` (or root effect) checks
  `window.location.hash` for `#auth_token=<jwt>`. If present:
  1. Write to `localStorage["argentum-session"]`.
  2. `history.replaceState(null, '', window.location.pathname + window.location.search)` to scrub.
- **Login button(s)**: Phase 1 shows "Sign in with Google" →
  `window.location.href = '/oauth2/authorization/google'`. The button list is data-driven so adding
  GitHub / Microsoft is a one-line change.
- **Session detection**: on boot, if a JWT exists in localStorage, `GET /api/me` with
  `Authorization: Bearer <jwt>` → store user in `authSlice`. 401 (expired/revoked) → clear the
  JWT, fall through to guest.
- **WebSocket connect**: include the JWT (if present) as the `token` field in
  `ClientMessage.Connect`. Guests keep using `localStorage["argentum-token"]`. The dev-scenario
  `?token=` URL param keeps working as it does today (server side decides whether the value is a
  JWT or a guest UUID).
- **Display name**: pre-fill the player-name input with `me.displayName` for authenticated users
  (still editable for the session — open question whether we want per-game name override now that
  accounts exist).
- **Logout**: `POST /auth/logout` (with the JWT) → clear `localStorage["argentum-session"]` and
  the `authSlice` → close + reopen the WebSocket so it reconnects as a guest.
- **Reverse proxy**: in dev, the Vite proxy must cover `/oauth2/**` and `/login/oauth2/**` so the
  redirect dance works from `localhost:5173`. No cookie forwarding needed. The Google OAuth client
  must whitelist `http://localhost:5173/login/oauth2/code/google` and the prod equivalent.

---

## Phase 2 — Deck Library Migration

Move the saved deck library from per-device `localStorage["argentum.decks"]` to a Postgres-backed
table keyed by `userAccountId`, and run a one-shot upload on first sign-in so existing users don't
lose what they've built.

### 2.1 `decks.decks` Schema

```sql
CREATE SCHEMA IF NOT EXISTS decks;

CREATE TABLE decks.decks (
    id                 UUID PRIMARY KEY,
    user_account_id    UUID NOT NULL REFERENCES auth.user_accounts(id) ON DELETE CASCADE,
    name               VARCHAR(120) NOT NULL,
    format             VARCHAR(32),                 -- nullable; matches client's optional field
    set_code           VARCHAR(8),                  -- for sealed/limited decks
    commander          VARCHAR(200),                -- nullable
    commander_printing VARCHAR(64),                 -- nullable
    cards              TEXT NOT NULL,               -- serialized deck body (see 2.2)
    schema_version     SMALLINT NOT NULL,           -- 1 or 2, preserving the client envelope
    created_at         TIMESTAMPTZ NOT NULL,
    updated_at         TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_decks_user ON decks.decks (user_account_id);
```

Notes:
- **`cards` is `TEXT` storing `kotlinx.serialization` JSON.** Keeps wire-compat with the existing
  client `SavedDeck` shape (`Record<cardName, count>` for v1, the entries array with per-card
  printing pins for v2). Promote to `JSONB` later (`ALTER TABLE … USING (cards::jsonb)`) when we
  need server-side card-level queries — none in scope here.
- **`ON DELETE CASCADE`** ties deck lifetime to the account. Account soft-delete (Phase 1.9
  `DELETE /api/me`) also cascade-deletes decks rather than orphaning them; same effect, no DB-level
  trigger needed.
- **`schema_version`** preserves the client's v1↔v2 envelope so we don't have to do a forced
  upgrade on import. Server treats `cards` as opaque blob; only the client cares about the inner
  shape.

### 2.2 Entity & Repository

```kotlin
@Entity
@Table(name = "decks", schema = "decks", indexes = [Index(columnList = "userAccountId")])
class Deck(
    @Id val id: UUID,
    @Column(nullable = false) val userAccountId: UUID,
    @Column(nullable = false) var name: String,
    var format: String?,
    var setCode: String?,
    var commander: String?,
    var commanderPrinting: String?,
    @Column(nullable = false, columnDefinition = "TEXT") var cards: String,   // kotlinx-serialized
    @Column(nullable = false) val schemaVersion: Short,
    val createdAt: Instant,
    var updatedAt: Instant
)
```

`DeckRepository : JpaRepository<Deck, UUID>` with:
- `findAllByUserAccountId(userId: UUID): List<Deck>`
- `findByIdAndUserAccountId(id: UUID, userId: UUID): Deck?` (so handlers can fail-closed without
  cross-user leaks even if the controller forgets to filter)

`DeckService` converts between the entity's `cards: String` and the client-facing `SavedDeck` DTO
via `kotlinx.serialization` (same `persistenceJson` instance the Redis repos use). No new
serializer.

### 2.3 REST Endpoints

```
GET    /api/decks                     List the current user's decks (lightweight summary)
GET    /api/decks/{id}                Get one deck's full content
POST   /api/decks                     Create a new deck
PUT    /api/decks/{id}                Replace a deck (atomic; updatedAt bumped)
DELETE /api/decks/{id}                Delete a deck
POST   /api/decks/import              Bulk import — used by the first-login upload flow (see 2.4)
```

All under the API chain (require a valid bearer JWT). 404 (not 403) when a deck ID doesn't belong
to the caller, to avoid leaking existence.

Summary shape (`GET /api/decks`):
```json
{ "id": "uuid", "name": "string", "format": "string|null", "cardCount": 60, "updatedAt": "iso8601" }
```

Full shape (`GET /api/decks/{id}` and `PUT /api/decks/{id}` body): mirrors the existing client
`SavedDeck` so the SPA can swap the storage backend without changing the shape it consumes.

### 2.4 First-Login Upload Flow

Triggered the first time the SPA sees a brand-new authenticated session **and** has non-empty
`localStorage["argentum.decks"]`:

1. After `GET /api/me` succeeds, the SPA reads `localStorage["argentum.decks"]` and parses the
   envelope.
2. The SPA calls `GET /api/decks` once. If the server-side library is empty, it sends `POST
   /api/decks/import` with the full local library as the body.
3. On success the SPA marks `localStorage["argentum.decks.uploaded_to_account"] = userAccountId`
   so we don't re-upload on every login. We do **not** delete the local copy — see migration
   strategy below.
4. From this point on, the SPA reads decks from the server (`GET /api/decks` / `GET /api/decks/{id}`)
   instead of from `localStorage["argentum.decks"]`.

Import endpoint is idempotent per `(userAccountId, deck.name)`: if a deck with the same name already
exists, the import skips it. Cheaper than UUID collision handling, and the v1/v2 deck IDs from the
client are device-local so we wouldn't trust them anyway. The server mints fresh UUIDs.

### 2.5 Frontend Refactor

- `web-client/src/store/deckLibrary.ts` — the existing Zustand store grows a backend mode.
  Authenticated users hit the new REST endpoints; guests keep their localStorage-only flow
  unchanged. Pseudocode:
  ```ts
  const isAuthed = !!useAuthSlice.getState().user
  saveDeck = isAuthed
      ? (d) => fetch('/api/decks', { method: 'POST', headers: bearer(), body: JSON.stringify(d) })
      : saveDeckLocally
  ```
- `DeckPicker` and `DeckBuilderOverlay` — no API changes required; they consume the Zustand store,
  which now decides where to read from.
- **Guest decks stay local.** Guests get the same per-device deck library as today
  (`localStorage["argentum.decks"]`). Logging in triggers the upload-on-first-login flow above.
- **Sealed/draft autosave** (`sessionStorage["argentum-deck-state"]`) stays purely client-side for
  both guests and authenticated users — it's an in-flight build, not a saved deck, and migrating it
  buys nothing.

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

| Component        | Purpose                                                       | Dev Setup                       | Prod Setup                       |
|------------------|---------------------------------------------------------------|---------------------------------|----------------------------------|
| **Postgres**     | `auth.user_accounts`, `decks.decks`, + future relational data | Docker Compose                  | Managed Postgres                 |
| **Flyway**       | Schema migrations                                             | Bundled in `bootRun`            | Bundled in deploy                |
| **OAuth IdPs**   | Identity providers (Google now; GitHub / Microsoft to follow) | OAuth client secret(s) in `.env` | Same, in deploy secret store    |
| **Redis**        | Game cache (already present) + JWT revocation list            | Already configured              | Already configured               |

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
3. **Future authenticated features** (profiles, match history, etc.) require an account — there is
   no server-side history to claim, so pre-account games stay anonymous forever.
4. **Deck library** is the exception (Phase 2): on first sign-in we upload the contents of
   `localStorage["argentum.decks"]` to the server via `POST /api/decks/import` and mark the local
   copy as "uploaded". The local copy is **not deleted** — kept as a read-only fallback for now in
   case the user logs out, and as belt-and-braces if the upload partially fails.
5. **Other client localStorage data is left in place.** Preferences (`argentum-auto-tap`,
   `argentum-stop-overrides`), display name (`argentum-player-name`), last lobby
   (`argentum-lobby-id`), background (`argentum-bg`) remain per-device. Moving these is not in
   scope; later features can migrate them individually.
6. **E2E scenarios** keep using the existing `?token=` + `DevScenarioController.preRegisterIdentity()`
   path verbatim; that is the guest path. No test rewrites required.

### Security Notes

- **Session JWT signing key**: HMAC secret in env (`GAME_SESSION_SECRET`), rotated by writing a new
  secret and accepting both old + new during a grace window. (Or RS256 with a JWKS endpoint if we
  want zero-downtime rotation built in.)
- **JWT in `localStorage` is XSS-exposed.** Any script running on our origin can read the JWT and
  exfiltrate it. We accept this risk for V1 because:
  - The product surface today is small (no payments, no third-party scripts).
  - React with the existing render patterns (no `dangerouslySetInnerHTML` on user input) gives
    decent baseline protection.
  - JWT lifetime is capped at 12h, limiting blast radius.
  - The server has a revocation list (Redis `jti` blocklist) so a leaked token can be killed.
  Tracked follow-up: migrate to `HttpOnly`, `Secure`, `SameSite=Lax` cookies. That requires
  tightening WebSocket origins (today `setAllowedOrigins("*")` — incompatible with cookie-bearing
  upgrades), exposing a `HandshakeInterceptor` to read the cookie at upgrade, and adding a
  `Set-Cookie` step to the success handler. Worth the work when we add anything sensitive
  (payments, real-time chat history, etc.).
- **CSRF**: not applicable in V1. Bearer tokens in `Authorization` headers don't auto-attach
  cross-origin and aren't sent on form posts. When we eventually move to cookies, revisit.
- **Open redirect**: the success handler must validate any post-login redirect target against an
  allowlist of own-origin paths.
- **Account takeover via email is not a thing here.** Accounts are keyed by `(provider, subject)`,
  never by email. A new sign-in is **never auto-linked** to an existing account by matching email
  — that would let a hostile IdP take over an account by claiming a victim's address. Different
  providers → different accounts. If account-linking is added later it must require the user to be
  authenticated with the first identity first.
- **Rate limiting**: `/oauth2/authorization/*`, `PATCH /api/me`, and `POST /api/decks/import` —
  basic per-IP throttling via a Spring filter.
- **Logout revocation**: blocklist the JWT's `jti` in Redis with TTL = remaining lifetime; the
  resource-server JWT decoder consults the blocklist on each request.

### Reconnection Semantics

| Scenario                                            | Outcome                                                                                                                                                                                  |
|-----------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Authenticated reconnect, same browser               | JWT in `localStorage["argentum-session"]` → included in `ClientMessage.Connect.token` → decoded → `PlayerIdentity` found via `identityByAccountId` → in-flight game/lobby restored.       |
| Authenticated reconnect, different browser/device   | The new device has no JWT until the user logs in there too. After login it has its own JWT pointing at the same `userAccountId`, so the server-side `PlayerIdentity` is resumed.          |
| Guest reconnect                                     | `localStorage["argentum-token"]` (legacy UUID) → existing token-based reconnect, unchanged from today.                                                                                    |
| Authenticated user clears `localStorage` mid-game   | Next `Connect` has no JWT → treated as guest → cannot resume; game auto-concedes after the existing grace period.                                                                         |
| Guest logs in mid-game                              | See Migration Strategy #2 — current game finishes as guest, next session is authenticated.                                                                                                |

### Scaling Notes

- `auth.user_accounts` is tiny and read-heavy. The `(provider, subject)` unique constraint and
  `lower(email)` index are sufficient indefinitely.
- `decks.decks` grows linearly with active users × decks-per-user. The `(user_account_id)` index
  handles the only access pattern (list-my-decks). Decks are small JSON blobs (a few KB each).
- JWT validation is local (no DB hit per request) — only logout and admin-role check hit Postgres;
  every request consults the Redis `jti` blocklist (single GET, sub-ms).
- Postgres connection pool size: 10 is plenty for auth + decks workload.

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

6. **Refresh tokens.** We intentionally do **not** store provider refresh tokens — re-auth at
   session expiry is acceptable for a game (re-login takes one click). Revisit only if 12-hour
   sessions become annoying.

7. **Multiple browser tabs.** Each tab opens its own WebSocket. Today this works because identities
   are token-keyed. With account-keyed auth, all tabs share an account; the `SessionRegistry`
   needs to decide whether to allow multiple concurrent WebSockets per `userAccountId` (probably
   yes — spectating one game while in another lobby) or enforce single-session (kick the older
   socket). Recommendation: allow multiple, but route `GameSession` participation through the
   `currentGameSessionId` field on `PlayerIdentity` so only one tab is the player.

8. **Deck upload conflict on first login.** If a user has already done first-login on device A and
   built decks server-side, then signs in on device B which still has its own
   `localStorage["argentum.decks"]` from before, what do we do? Default: import the local decks if
   they don't conflict by name; otherwise skip them and show a "couldn't import N decks (name
   conflict)" toast. Don't merge automatically — name conflicts almost certainly mean different
   decks despite the shared name.

9. **`AccountStatus.SUSPENDED`.** Listed in the enum for future use but no V1 trigger
   (admin action vs auto-abuse-detection). Drop the value from V1 and reintroduce when there's a
   real use case, or keep it as a placeholder. Currently leaning toward **drop until needed.**

10. **GDPR right-to-be-forgotten vs soft-delete.** `DELETE /api/me` is currently soft-delete +
    anonymize, which is generally accepted as compliant when PII fields are nulled. If we ever
    need hard deletion (e.g. explicit right-to-erasure request that doesn't accept anonymization),
    add a separate admin path that cascades through `auth.user_accounts` and any FK-linked tables.
