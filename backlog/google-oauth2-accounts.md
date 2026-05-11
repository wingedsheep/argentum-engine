# Google OAuth2 Accounts & Persistent Identity

A direct Google OAuth2 + Spring Security implementation that replaces the current ephemeral
`PlayerIdentity` (random UUID in `localStorage`) with persistent user accounts backed by Postgres.
Provides the account foundation needed for leagues, seasons, player profiles, and any other feature
that requires durable cross-session identity — while preserving anonymous guest play for casual
games and dev scenario E2E tests.

## Supersedes

This backlog item **replaces Phase 1 of [league-season-system.md](league-season-system.md)** —
the original plan called for Keycloak SSO. We are dropping Keycloak in favour of a direct Google
OAuth2 integration using `spring-boot-starter-oauth2-client` / `spring-boot-starter-oauth2-resource-server`.

**Rationale for direct Google OAuth over Keycloak:**

| Concern              | Keycloak                                            | Direct Google OAuth                                 |
|----------------------|-----------------------------------------------------|-----------------------------------------------------|
| Infra cost           | Extra service to deploy, upgrade, back up           | None — Google is the IdP                            |
| Dev ergonomics       | Docker Compose service + realm/client config        | Two env vars (`client-id`, `client-secret`)         |
| User experience      | Google login via intermediate Keycloak redirect     | One redirect, no extra branding                     |
| Multi-provider later | Trivial (Keycloak federates Discord/GitHub natively) | Requires per-provider Spring registration           |
| Token model          | Keycloak issues JWTs, we validate via JWKS          | We issue our own session/JWT after OAuth callback   |
| Lock-in              | Keycloak schema, admin API                          | Standard Spring Security; provider is swappable     |

For a small player base with one realistic IdP for now (Google), the operational simplicity of
direct OAuth outweighs the federation flexibility Keycloak offers. If we later want Discord/GitHub
login we add another `ClientRegistration` — Spring Security supports N providers out of the box.
`league-season-system.md` Phase 1 has been updated to point at this file as the authoritative auth plan.

## Dependencies

This feature depends on:
- **Postgres** (Phase 1.1 below) — first relational store in the project; everything user-keyed
  flows from here.
- **PlayerIdentity state machine formalization**
  ([architecture #6](archived/game-server-architecture.md#6-formalize-playeridentity-state-machine)) —
  the auth flow needs a cleaner identity lifecycle than the current ad-hoc model.

This feature is a prerequisite for:
- Leagues & seasons ([league-season-system.md](league-season-system.md) Phase 2+)
- Player profiles, deck library, match history
- Any feature that needs durable cross-device identity

---

## Phase 1 — Infrastructure & Persistence

### 1.1 Postgres Setup

- Add Postgres service to `docker-compose.local.yml` (and prod `docker-compose.yml`) alongside Redis.
- Dev defaults: `postgres:16-alpine`, port `5432`, db `argentum`, user `argentum`, password from env.
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
- First migration `V1__user_accounts.sql` creates the schema described in Phase 1.4.
- All future schema (leagues, seasons, etc.) live as further `V*__*.sql` files in the same dir.

### 1.3 Data Access Layer

Choose **Spring Data JPA** (rather than Exposed) for consistency with the existing Spring stack and
because there is no incumbent Kotlin DSL preference in the codebase.

Add to `game-server/build.gradle.kts`:
```kotlin
implementation("org.springframework.boot:spring-boot-starter-data-jpa")
implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
implementation("org.springframework.boot:spring-boot-starter-security")
runtimeOnly("org.postgresql:postgresql")
implementation("org.flywaydb:flyway-core")
runtimeOnly("org.flywaydb:flyway-database-postgresql")
implementation("com.auth0:java-jwt:4.x")    // for issuing our own session JWTs (see 1.6)
```

### 1.4 UserAccount Entity

```kotlin
@Entity
@Table(name = "user_accounts")
class UserAccount(
    @Id val id: UUID,                            // our own UUID, generated on JIT provision
    @Column(nullable = false, unique = true)
    val googleSubject: String,                   // Google `sub` claim — stable per-Google-account
    @Column(nullable = false)
    var email: String,                           // mutable: Google can change primary email
    @Column(nullable = false)
    var displayName: String,                     // editable by user; defaults to Google name
    var avatarUrl: String?,                      // Google `picture` claim
    val createdAt: Instant,
    var lastLoginAt: Instant,
    @Enumerated(EnumType.STRING)
    var status: AccountStatus = AccountStatus.ACTIVE   // ACTIVE, SUSPENDED, DELETED
)
```

Initial migration (`V1__user_accounts.sql`):
```sql
CREATE TABLE user_accounts (
    id              UUID PRIMARY KEY,
    google_subject  VARCHAR(64)  NOT NULL UNIQUE,
    email           VARCHAR(320) NOT NULL,
    display_name    VARCHAR(80)  NOT NULL,
    avatar_url      TEXT,
    created_at      TIMESTAMPTZ  NOT NULL,
    last_login_at   TIMESTAMPTZ  NOT NULL,
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE'
);
CREATE INDEX idx_user_accounts_email ON user_accounts (lower(email));
```

`UserAccountRepository : JpaRepository<UserAccount, UUID>` exposes `findByGoogleSubject(sub)`.

### 1.5 OAuth2 Login Flow (Spring Security)

Configure two Spring Security filter chains:

1. **OAuth2 login chain** — `/auth/**` endpoints handle the browser-initiated Google OAuth2 flow.
2. **API/WebSocket chain** — `/api/**` and `/game` validate our own session token (see 1.6).

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
        .securityMatcher("/auth/**", "/login/**", "/oauth2/**")
        .oauth2Login { o -> o
            .userInfoEndpoint { it.userService(googleOidcUserService()) }
            .successHandler(::onLoginSuccess)
            .failureHandler(::onLoginFailure)
        }
        .csrf { it.disable() }      // SPA — CSRF handled via SameSite cookie + token
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

Spring `application.yml`:
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
```

### 1.6 JIT Provisioning & Session Token Issuance

When a user completes Google OAuth2 the success handler runs:

```
1. Spring's oauth2Login filter validates the Google id_token and gives us an OidcUser.
2. AccountProvisioner.findOrCreate(oidcUser):
     - lookup user_accounts.google_subject = oidcUser.sub
     - if missing: insert new row (id = UUID.randomUUID(), createdAt = now)
     - if present: update email/avatarUrl if changed, set lastLoginAt = now
3. SessionTokenService.issue(userAccount):
     - sign a self-issued JWT (HS256 with a server-side secret, or RS256 if we want key rotation)
     - claims: sub = userAccount.id, name = displayName, exp = now+12h, iss = "argentum"
4. Set the JWT as an HttpOnly, Secure, SameSite=Lax cookie `argentum_session`.
     (HttpOnly so JS can't read it; SameSite=Lax so it survives the redirect from Google.)
5. Redirect to the SPA: 302 -> https://app/<original_target_or_/>
```

We issue **our own JWT** rather than passing Google's `id_token` to the SPA. Reasons:
- The id_token contains PII (email) that we don't want exposed to the frontend cookie surface beyond
  what we choose; our session JWT only carries `userAccountId` + `displayName`.
- We control rotation and revocation. (See 1.10 — a revocation list in Redis for forced logout.)
- Refresh is decoupled from Google: a long-lived refresh token isn't needed in the browser; the
  user re-authenticates with Google after the session JWT expires.

### 1.7 WebSocket Authentication Bridge

This is the trickiest part. `WebSocketConfig` registers `/game` with `setAllowedOrigins("*")` and
`GameWebSocketHandler` currently authenticates on the first `ClientMessage.Connect`. WebSocket
upgrades cannot easily participate in the OAuth2 redirect flow (browsers don't expose `Authorization`
headers to `new WebSocket()`), so we keep auth in-message but **read the session cookie at upgrade
time** and attach the resolved account.

Implementation:

1. Add a `HandshakeInterceptor` to `WebSocketConfig`:
   ```kotlin
   registry.addHandler(gameWebSocketHandler, "/game")
       .addInterceptors(SessionCookieHandshakeInterceptor(sessionTokenService))
       .setAllowedOriginPatterns(...)        // tightened from "*" — must be exact origins for cookies
   ```
   The interceptor reads the `argentum_session` cookie from the upgrade request, validates it, and
   stashes the resolved `UserAccount` in `WebSocketSession.attributes["account"]`. If the cookie is
   absent/invalid the WebSocket still opens — the connection becomes a **guest session**.

2. `ConnectionHandler.handleConnect()` becomes:
   ```kotlin
   val account = session.attributes["account"] as UserAccount?
   when {
       account != null     -> handleAuthenticatedConnect(session, account, message)
       message.token != null -> handleLegacyTokenReconnect(session, message)   // existing path
       else                -> handleGuestConnect(session, message)              // new explicit path
   }
   ```

3. For authenticated connects the `PlayerIdentity` is keyed by `account.id` instead of a random
   UUID. We add a new field:
   ```kotlin
   class PlayerIdentity(
       val token: String = UUID.randomUUID().toString(),
       val userAccountId: UUID? = null,        // null for guests
       ...
   )
   ```
   `SessionRegistry` gains an `identityByAccountId: ConcurrentHashMap<UUID, PlayerIdentity>`
   index so an authenticated reconnect on a new browser still finds the same identity (rather than
   relying on `localStorage` survival).

4. Reconnect resolution order:
   1. If `session.attributes["account"]` set → look up identity by `userAccountId` (cross-device
      reconnect works).
   2. Else if `ClientMessage.Connect.token` present → existing token-based reconnect (guest path).
   3. Else → fresh guest identity.

### 1.8 Guest Mode

Anonymous play continues to work and is explicitly supported:

- WebSocket upgrade without a session cookie is allowed.
- The client sends `ClientMessage.Connect(playerName, token=…)` exactly as today.
- `PlayerIdentity.userAccountId` is null; `SessionRegistry` still indexes by `token`.
- Server-issued token is returned in `ServerMessage.Connected` and stored in `localStorage`.
- League/season/profile REST endpoints **reject** unauthenticated requests (`401`); gameplay
  endpoints (lobby create, quick-game, dev scenarios) remain open.

This preserves:
- Drop-in casual play (no account barrier).
- Existing E2E scenario flows in `e2e-scenarios/` that rely on `?token=` URL params and pre-registered
  identities via `DevScenarioController`.
- The `X-Admin-Password` flow on `/api/admin/**` continues to work in parallel for now (see 1.10
  for the admin migration plan).

### 1.9 REST Endpoints

```
GET    /auth/login/google          Initiates Google OAuth2 (Spring redirects to Google)
GET    /login/oauth2/code/google   OAuth2 callback (Spring handles, we run successHandler)
POST   /auth/logout                Clears argentum_session cookie + revokes JWT (Redis blocklist)
GET    /api/me                     Returns the current UserAccount (or 401 if guest)
PATCH  /api/me                     Update displayName (only mutable field by user)
DELETE /api/me                     Soft-delete: status=DELETED, anonymize email/displayName
```

`/api/me` shape:
```json
{
  "id": "uuid",
  "displayName": "string",
  "email": "string",
  "avatarUrl": "string|null",
  "createdAt": "iso8601"
}
```

### 1.10 Roles & Admin Migration

- Add `user_accounts.roles VARCHAR[]` (Postgres array) or a `user_roles` join table for `ADMIN`.
- The `X-Admin-Password` check in `AdminController.checkAuth()` is preserved as a fallback during
  rollout but is deprecated; admin access also accepts a session JWT with the `ADMIN` role.
- A bootstrap admin can be promoted via a one-time `SQL` migration referencing their `google_subject`,
  or via an env-configured allowlist `GAME_ADMIN_EMAILS` checked at JIT-provision time.
- Schedule removal of the `X-Admin-Password` header as a separate cleanup item once at least one
  admin account exists and the admin UI sends the session cookie.

### 1.11 Frontend Integration

`web-client` changes (`src/store/slices/connectionSlice.ts`, `connectionHandlers.ts`,
`src/components/landing/`):

- **Login button**: top-right "Sign in with Google" → `window.location.href = '/auth/login/google'`.
  Spring handles the redirect dance; the SPA never sees Google.
- **Session detection**: on app boot, `GET /api/me`. 200 → store user in a new `authSlice`. 401 →
  user is a guest (current behaviour).
- **WebSocket connect**: drop the `?token=` URL param for authenticated users — the cookie travels
  on the WS upgrade automatically. Guests keep the current `localStorage['argentum-token']` path.
- **Display name**: pre-fill the player-name input with `me.displayName` for authenticated users
  (still editable for the session — though we should consider whether per-game name override is
  desirable now that accounts exist).
- **Logout**: `POST /auth/logout` → clear `authSlice` → reload to drop the WebSocket session.
- **Reverse proxy**: in dev, the Vite proxy (`/game`, `/api`) must also proxy `/auth` and
  `/login/oauth2` and forward cookies (`Set-Cookie` rewriting for the `localhost:5173` origin).
  The Google OAuth client must whitelist `http://localhost:5173/login/oauth2/code/google` and
  the prod origin.

---

## Technical Considerations

### Infrastructure Additions

| Component    | Purpose                                            | Dev Setup                 | Prod Setup                  |
|--------------|----------------------------------------------------|---------------------------|-----------------------------|
| **Postgres** | UserAccount + future relational data               | Docker Compose            | Managed Postgres            |
| **Flyway**   | Schema migrations                                  | Bundled in `bootRun`      | Bundled in deploy           |
| **Google**   | OAuth2 identity provider                           | OAuth Client Secret in `.env` | Same, in deploy secret store |
| **Redis**    | Game cache (already present) + JWT revocation list | Already configured        | Already configured           |

### Migration Strategy from Ephemeral PlayerIdentity

The current ephemeral `PlayerIdentity` (random UUID in `localStorage`) must coexist during transition:

1. **Phase 1 ships both paths**: cookie-bearing connections become authenticated identities;
   cookie-less connections remain guest identities backed by the `localStorage` token. No data
   migration is needed because no per-user durable data exists yet.
2. **Guest → Account merge**: when a guest player logs in, we have an open question (see Risks)
   about whether to migrate the current `PlayerIdentity.token` mapping to the new
   `userAccountId`-keyed identity, or to start a fresh authenticated identity and let the in-flight
   game finish under the guest token. Recommended default: **start a fresh authenticated identity
   on next connect; let the in-flight guest session complete naturally**.
3. **League/season features (Phase 2+ of league-season-system.md)** require an authenticated
   account — there is no upgrade path for past anonymous match history to be claimed (acceptable
   trade-off; pre-account games stay anonymous forever).
4. **E2E scenarios** keep using the existing `?token=` + `DevScenarioController.preRegisterIdentity()`
   path verbatim; that is the guest path. No test rewrites required.

### Security Notes

- **Session JWT signing key**: HMAC secret in env (`GAME_SESSION_SECRET`), rotated by writing a new
  secret and accepting both old + new during a grace window. (Or RS256 with a JWKS endpoint if we
  want zero-downtime rotation built in.)
- **Cookie flags**: `HttpOnly`, `Secure` (prod), `SameSite=Lax`, `Path=/`, 12-hour `Max-Age`.
- **CSRF**: REST + WebSocket are stateless and bearer-token-like via the cookie. Lax SameSite blocks
  cross-site form posts. State-changing endpoints (`PATCH /api/me`, `DELETE /api/me`, league
  mutations later) should additionally require a `X-Requested-With: XMLHttpRequest` header to
  defeat SameSite=Lax loopholes via top-level GET redirects (which we don't accept POSTs for anyway).
- **CORS**: the current `setAllowedOrigins("*")` on the WebSocket handler is **incompatible with
  cookie-bearing upgrades** — browsers refuse to send cookies cross-origin to wildcard origins.
  Switch to `setAllowedOriginPatterns(env-driven exact list)`.
- **Open redirect**: the success handler must validate any post-login redirect URL against an
  allowlist of own-origin paths.
- **Account takeover via email reuse**: do not trust `email` as the lookup key — always
  `google_subject`. (Email can change; sub cannot.)
- **Rate limiting**: `/auth/login/google` and `/api/me` PATCH — basic per-IP throttling via a
  Spring filter.
- **Logout revocation**: blocklist the JWT's `jti` in Redis with TTL = remaining lifetime; the
  resource-server JWT decoder consults the blocklist.

### Reconnection Semantics

| Scenario                                           | Outcome                                                    |
|----------------------------------------------------|------------------------------------------------------------|
| Authenticated reconnect, same browser              | Cookie present → resolved at upgrade → `PlayerIdentity` found by `userAccountId` → existing in-flight game/lobby restored |
| Authenticated reconnect, different browser/device  | Cookie present (after re-login) → same `userAccountId` → same `PlayerIdentity` resumed (works today only if `localStorage` survived; now works cross-device) |
| Guest reconnect                                    | `localStorage` token → existing `PlayerIdentity` (unchanged from today) |
| Authenticated user clears cookies mid-game         | Treated as guest on reconnect → cannot resume; game auto-concedes after grace period (current behaviour) |
| Guest logs in mid-game                             | See Migration Strategy point 2 — current game finishes as guest, future connects authenticated |

### Scaling Notes

- `user_accounts` is tiny and read-heavy. A simple b-tree on `google_subject` + `lower(email)` is
  sufficient indefinitely.
- JWT validation is local (no DB hit per request) — only logout and admin-role check hit Postgres.
- Postgres connection pool size: 10 is plenty until leagues launch.

---

## Open Questions / Risks

1. **Guest → account merge UX.** If a guest is mid-game when they hit "Sign in with Google", do we
   (a) defer login until game-over, (b) silently swap the underlying identity (risky — `EntityId`
   for the player is engine-internal and may be referenced from `GameState`), or (c) finish the
   guest game and start the next session authenticated? Default proposed: (c). Decide before
   shipping the login button.

2. **WebSocket origin tightening breaks E2E?** Switching from `setAllowedOrigins("*")` to an exact
   list may break Playwright runs that connect to `localhost:8080` from arbitrary ports. Plan: add
   a `game.dev-endpoints.allowedWsOrigins` config and default to `["*"]` only when
   `dev-endpoints.enabled=true`.

3. **Single device / single account during transition.** A user who has been playing as a guest
   with `localStorage` data, then signs in, won't see their guest history (there is no history
   yet, but this matters once leagues exist). Documented behaviour: pre-account play is
   permanently anonymous, no claiming.

4. **Display-name uniqueness.** Multiple Google accounts can share a display name. Do we enforce
   uniqueness? Recommendation: **no** — display names are not identifiers, the UUID is. We can
   show a `#1234` suffix or partial email in disambiguating contexts (e.g. league member list).

5. **Email change handling.** If Google's primary email changes between logins, we update the row
   in JIT provisioning. This is silent; surface it in `/api/me` so the user sees it on next view.

6. **Admin migration cutover.** Until we promote at least one admin to a `UserAccount` with the
   `ADMIN` role, the `X-Admin-Password` header is the only admin path. Don't remove the header
   check in Phase 1 — schedule its removal as a separate cleanup item once an admin account exists
   and the replay UI sends the session cookie.

7. **Refresh tokens.** We intentionally do **not** store Google refresh tokens — re-auth at session
   expiry is acceptable for a game (re-login takes one click). Revisit only if 12-hour sessions
   become annoying.

8. **Multiple browser tabs.** Each tab opens its own WebSocket. Today this works because identities
   are token-keyed. With cookie-keyed auth, all tabs share an account; the `SessionRegistry`
   needs to decide whether to allow multiple concurrent WebSockets per `userAccountId` (probably
   yes — spectating one game while in another lobby) or enforce single-session (kick the older
   socket). Recommendation: allow multiple, but route `GameSession` participation through the
   `currentGameSessionId` field on `PlayerIdentity` so only one tab is the player.

---

## Phase 2+

Phases 2 (Leagues), 3 (Seasons & Standings), and 4 (Social & Polish) from
[league-season-system.md](league-season-system.md) remain valid and depend on this phase.
