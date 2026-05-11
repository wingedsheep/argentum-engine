# OAuth2 Accounts & Persistent Identity

A direct OAuth2 / OIDC + Spring Security implementation that replaces the current ephemeral
`PlayerIdentity` (random UUID in `localStorage`) with persistent user accounts backed by Postgres.
**Google** is the first wired-up provider; **GitHub** and **Microsoft** are designed-for follow-ups
that drop in via a new `ClientRegistration` and a per-provider claim mapper. Provides a durable
cross-session identity for any feature that needs one — while preserving anonymous guest play for
casual games and dev scenario E2E tests.

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

- **Phase 1 ships Google sign-in end-to-end.**
- **Schema and abstractions are provider-agnostic from day one.** Each provider is identified by a
  `(provider, subject)` tuple; the JIT-provisioning flow goes through a `ProviderClaimMapper`
  abstraction so adding GitHub / Microsoft is purely additive (no migrations, no rewiring).
- **Each provider gets a separate account.** Signing in with Google produces one
  `auth.user_accounts` row; later signing in with GitHub as the same human produces a second row.
  See [Open Questions](#open-questions--risks) for the rationale and the linking-as-follow-up note.

## Dependencies

This feature depends on:
- **Postgres** (Phase 1.1 below) — first relational store in the project; everything user-keyed
  flows from here.
- **PlayerIdentity state machine formalization**
  ([architecture #6](archived/game-server-architecture.md#6-formalize-playeridentity-state-machine)) —
  the auth flow needs a cleaner identity lifecycle than the current ad-hoc model.

This feature is a prerequisite for any feature that needs durable cross-device identity (player
profiles, deck library, match history, etc.).

---

## Phase 1 — Infrastructure & Persistence

### 1.1 Postgres Setup

- Add Postgres service to `docker-compose.local.yml` (and prod `docker-compose.yml`) alongside Redis.
- Dev defaults: `postgres:16-alpine`, port `5432`, db `argentum`, user `argentum`, password from env.
- **One shared database, per-feature Postgres schemas (namespaces).** Auth tables live in an `auth`
  schema; future features get their own schema. Keeps related tables grouped, makes migrations
  per-feature, and lets us grant scoped DB roles later without splitting databases.
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
- First migration `V1__auth_schema.sql` creates the `auth` schema and the `user_accounts` table
  (see Phase 1.4 for the DDL).
- Future migrations live as further `V*__*.sql` files in the same directory, each creating and
  populating its own Postgres schema for the feature it owns.

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
    var status: AccountStatus = AccountStatus.ACTIVE   // ACTIVE, SUSPENDED, DELETED
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
        .securityMatcher("/oauth2/**", "/login/oauth2/**", "/auth/**")
        .oauth2Login { o -> o
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
     - sign a self-issued JWT (HS256 with a server-side secret, or RS256 if we want key rotation)
     - claims: sub = userAccount.id, name = displayName, prv = userAccount.provider,
               exp = now+12h, iss = "argentum"

5. Set the JWT as an HttpOnly, Secure, SameSite=Lax cookie `argentum_session`.
     (HttpOnly so JS can't read it; SameSite=Lax so it survives the redirect from the provider.)

6. Redirect to the SPA: 302 -> https://app/<original_target_or_/>
```

We issue **our own JWT** rather than passing the provider's `id_token`/access token to the SPA:
- The provider token contains PII and provider-specific claims that we don't want on the frontend
  cookie surface; our session JWT only carries `userAccountId`, `displayName`, and `provider`.
- We control rotation and revocation. (See 1.10 — revocation list in Redis for forced logout.)
- Refresh is decoupled from the provider: a long-lived refresh token isn't needed in the browser;
  the user re-authenticates when the session JWT expires (one click).

### 1.7 WebSocket Authentication Bridge

This is the trickiest part. `WebSocketConfig` registers `/game` with `setAllowedOrigins("*")` and
`GameWebSocketHandler` currently authenticates on the first `ClientMessage.Connect`. WebSocket
upgrades cannot easily participate in the OAuth2 redirect flow (browsers don't expose
`Authorization` headers to `new WebSocket()`), so we keep auth in-message but **read the session
cookie at upgrade time** and attach the resolved account.

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
- Account-scoped REST endpoints (`/api/me`, etc.) **reject** unauthenticated requests (`401`);
  gameplay endpoints (lobby create, quick-game, dev scenarios) remain open.

This preserves:
- Drop-in casual play (no account barrier).
- Existing E2E scenario flows in `e2e-scenarios/` that rely on `?token=` URL params and pre-registered
  identities via `DevScenarioController`.
- The `X-Admin-Password` flow on `/api/admin/**` continues to work in parallel for now (see 1.10
  for the admin migration plan).

### 1.9 REST Endpoints

Spring's defaults handle the OAuth dance per registration; the per-provider URLs are derived from
`registrationId`:

```
GET    /oauth2/authorization/google     Initiates Google OAuth2 (Spring redirects)
GET    /login/oauth2/code/google        Callback (Spring handles, we run successHandler)
                                        (mirror routes appear automatically for /github, /microsoft)
POST   /auth/logout                     Clears argentum_session cookie + revokes JWT (Redis blocklist)
GET    /api/me                          Returns the current UserAccount (or 401 if guest)
PATCH  /api/me                          Update displayName (only mutable field by user)
DELETE /api/me                          Soft-delete: status=DELETED, anonymize email/displayName
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
  rollout but is deprecated; admin access also accepts a session JWT with the `ADMIN` role.
- A bootstrap admin can be promoted via a one-time SQL migration referencing their
  `(provider, subject)`, or via env-configured allowlists (e.g. `GAME_ADMIN_GOOGLE_EMAILS`) checked
  at JIT-provision time.
- Schedule removal of the `X-Admin-Password` header as a separate cleanup item once at least one
  admin account exists and the admin UI sends the session cookie.

### 1.11 Frontend Integration

`web-client` changes (`src/store/slices/connectionSlice.ts`, `connectionHandlers.ts`,
`src/components/landing/`):

- **Login button(s)**: Phase 1 shows "Sign in with Google" →
  `window.location.href = '/oauth2/authorization/google'`. The button list is data-driven so adding
  GitHub / Microsoft is a one-line change (target URL: `/oauth2/authorization/{provider}`).
- **Session detection**: on app boot, `GET /api/me`. 200 → store user (incl. `provider`) in a new
  `authSlice`. 401 → user is a guest (current behaviour).
- **WebSocket connect**: drop the `?token=` URL param for authenticated users — the cookie travels
  on the WS upgrade automatically. Guests keep the current `localStorage['argentum-token']` path.
- **Display name**: pre-fill the player-name input with `me.displayName` for authenticated users
  (still editable for the session — though we should consider whether per-game name override is
  desirable now that accounts exist).
- **Logout**: `POST /auth/logout` → clear `authSlice` → reload to drop the WebSocket session.
- **Reverse proxy**: in dev, the Vite proxy (`/game`, `/api`) must also proxy `/oauth2` and
  `/login/oauth2` and forward cookies (`Set-Cookie` rewriting for the `localhost:5173` origin).
  Each OAuth provider's console must whitelist
  `http://localhost:5173/login/oauth2/code/{provider}` and the prod equivalent.

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
| **Postgres**     | UserAccount + future relational data                          | Docker Compose                  | Managed Postgres                 |
| **Flyway**       | Schema migrations                                             | Bundled in `bootRun`            | Bundled in deploy                |
| **OAuth IdPs**   | Identity providers (Google now; GitHub / Microsoft to follow) | OAuth client secret(s) in `.env` | Same, in deploy secret store    |
| **Redis**        | Game cache (already present) + JWT revocation list            | Already configured              | Already configured               |

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
3. **Future authenticated features** (profiles, match history, etc.) require an account — there is
   no upgrade path for past anonymous play to be claimed (acceptable trade-off; pre-account games
   stay anonymous forever).
4. **E2E scenarios** keep using the existing `?token=` + `DevScenarioController.preRegisterIdentity()`
   path verbatim; that is the guest path. No test rewrites required.

### Security Notes

- **Session JWT signing key**: HMAC secret in env (`GAME_SESSION_SECRET`), rotated by writing a new
  secret and accepting both old + new during a grace window. (Or RS256 with a JWKS endpoint if we
  want zero-downtime rotation built in.)
- **Cookie flags**: `HttpOnly`, `Secure` (prod), `SameSite=Lax`, `Path=/`, 12-hour `Max-Age`.
- **CSRF**: REST + WebSocket are stateless and bearer-token-like via the cookie. Lax SameSite blocks
  cross-site form posts. State-changing endpoints (`PATCH /api/me`, `DELETE /api/me`, and other
  account mutations) should additionally require an `X-Requested-With: XMLHttpRequest` header to
  defeat SameSite=Lax loopholes via top-level GET redirects (which we don't accept POSTs for anyway).
- **CORS**: the current `setAllowedOrigins("*")` on the WebSocket handler is **incompatible with
  cookie-bearing upgrades** — browsers refuse to send cookies cross-origin to wildcard origins.
  Switch to `setAllowedOriginPatterns(env-driven exact list)`.
- **Open redirect**: the success handler must validate any post-login redirect URL against an
  allowlist of own-origin paths.
- **Account takeover via email is not a thing here.** Accounts are keyed by `(provider, subject)`,
  never by email. A new sign-in is **never auto-linked** to an existing account by matching email
  — that would let a hostile IdP take over an account by claiming a victim's address. Different
  providers → different accounts. If account-linking is added later it must require the user to be
  authenticated with the first identity first.
- **Rate limiting**: `/oauth2/authorization/*` and `PATCH /api/me` — basic per-IP throttling via a
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

- `auth.user_accounts` is tiny and read-heavy. A simple b-tree on `(provider, subject)` (the unique
  constraint) plus `lower(email)` is sufficient indefinitely.
- JWT validation is local (no DB hit per request) — only logout and admin-role check hit Postgres.
- Postgres connection pool size: 10 is plenty for an auth-only workload.

---

## Open Questions / Risks

1. **Guest → account merge UX.** If a guest is mid-game when they hit "Sign in with …", do we
   (a) defer login until game-over, (b) silently swap the underlying identity (risky — `EntityId`
   for the player is engine-internal and may be referenced from `GameState`), or (c) finish the
   guest game and start the next session authenticated? Default proposed: (c). Decide before
   shipping the login buttons.

2. **WebSocket origin tightening breaks E2E?** Switching from `setAllowedOrigins("*")` to an exact
   list may break Playwright runs that connect to `localhost:8080` from arbitrary ports. Plan: add
   a `game.dev-endpoints.allowedWsOrigins` config and default to `["*"]` only when
   `dev-endpoints.enabled=true`.

3. **Single device / single account during transition.** A user who has been playing as a guest
   with `localStorage` data, then signs in, won't see their guest history. Documented behaviour:
   pre-account play is permanently anonymous, no claiming.

4. **Same human across providers = separate accounts.** Signing in with Google then later with
   GitHub creates two `auth.user_accounts` rows. Trade-off: a simpler, safer model than
   email-based auto-linking (which is exploitable — see Security Notes). If we later want a single
   account with multiple linked sign-ins, it's a follow-up feature: an authenticated user adds
   another identity, which inserts a row into a separate `auth.linked_identities` table; the
   schema added in Phase 1 stays compatible.

5. **Display-name uniqueness.** Multiple accounts can share a display name (especially across
   providers). Do we enforce uniqueness? Recommendation: **no** — display names are not
   identifiers, the UUID is. We can show a `#1234` suffix or provider badge in disambiguating
   contexts.

6. **Email change handling.** If the provider's primary email changes between logins, we update
   the row in JIT provisioning. This is silent; surface it in `/api/me` so the user sees it on
   next view.

7. **Admin migration cutover.** Until we promote at least one admin to a `UserAccount` with the
   `ADMIN` role, the `X-Admin-Password` header is the only admin path. Don't remove the header
   check in Phase 1 — schedule its removal as a separate cleanup item once an admin account exists
   and the replay UI sends the session cookie.

8. **Refresh tokens.** We intentionally do **not** store provider refresh tokens — re-auth at
   session expiry is acceptable for a game (re-login takes one click). Revisit only if 12-hour
   sessions become annoying.

9. **Multiple browser tabs.** Each tab opens its own WebSocket. Today this works because identities
   are token-keyed. With cookie-keyed auth, all tabs share an account; the `SessionRegistry`
   needs to decide whether to allow multiple concurrent WebSockets per `userAccountId` (probably
   yes — spectating one game while in another lobby) or enforce single-session (kick the older
   socket). Recommendation: allow multiple, but route `GameSession` participation through the
   `currentGameSessionId` field on `PlayerIdentity` so only one tab is the player.

10. **Filename.** The file is still named `google-oauth2-accounts.md` for now even though the scope
    broadened. Renaming to `oauth2-accounts.md` is a one-line `git mv` whenever convenient.
