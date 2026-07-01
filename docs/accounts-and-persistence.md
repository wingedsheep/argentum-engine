# Accounts & persistence (PostgreSQL)

An **opt-in** accounts subsystem for the game server: passwordless magic-link sign-in, server-side
saved decks, and win/loss stats. It is off by default — with it disabled the server behaves exactly
as before (anonymous play, in-memory/Redis state, no database). All of it lives in `game-server`;
the rules engine is untouched.

> Design notes / decisions are tracked in memory `project_accounts_auth_persistence`. Persistence
> uses **Spring Data JDBC** (not JPA) — explicit queries, no lazy-loading surprises.

## What it adds

- **Accounts** keyed by email; no passwords (magic link). The account **id is a UUID** (since V4) and
  doubles as a shareable, non-guessable **friend code** — you invite a friend by their id, never their
  email.
- **Friends** — request → accept friendships, an unfriend action, live **online presence**, and a
  per-account **hide-my-online-status** toggle (see "Friends & presence" below).
- **Saved decks** stored per account (the deckbuilder's `SharedDeck` JSON), reachable from any device.
- **Stats** — one row per finished game; per-account win/loss, preferred colors/sets, game modes
  played, head-to-head vs specific opponents, and a game history, all computed on demand.
- **Deck contents** of every recorded game are stored card-by-card, so we can compute the most-played
  cards and per-card win rates.
- **Tournaments** are recorded across their whole lifecycle: an `IN_PROGRESS` row is written when the
  bracket goes live, flipped to `COMPLETED` (with final standings) when it finishes, or `ABANDONED` if
  the lobby is torn down first. This means the admin dashboard and player profiles see running and
  abandoned tournaments, not only finished ones. The row is keyed by lobby id and upserted in place.
- **Admin dashboard** — global totals, games-per-day, mode/color distributions, top cards, recorded
  tournaments, and an IP-based geolocation estimate of where players connect from.
- **Guest play still works** — login is optional. Per-account stats need an account, but guest games
  (not AI) still count toward the global admin stats and geolocation.

A game is only recorded if it reached a winner **or** had more than a trivial number of actions
(`frameCount >= 10`), and only if at least one seat is a human (AI-only games are skipped).

Redis stays responsible for hot/ephemeral game/lobby/tournament state. Postgres only holds durable,
user-owned data. Different lifecycles, deliberately not merged.

## Enabling it

Set these (e.g. in `.env` for `just server`, or the deploy environment):

```bash
ACCOUNTS_ENABLED=true
ACCOUNTS_AUTOCONFIG_EXCLUDE=          # MUST be cleared (empty) — see "Gating" below
ACCOUNTS_DB_URL=jdbc:postgresql://localhost:5432/argentum
ACCOUNTS_DB_USER=argentum
ACCOUNTS_DB_PASSWORD=argentum
ACCOUNTS_AUTH_SECRET=<long-random-string>   # blank => random per restart (dev only)
APP_BASE_URL=http://localhost:5173          # origin the magic link points at
ACCOUNTS_FROM_EMAIL=no-reply@wingedsheep.com
# Mailgun SMTP — leave MAIL_USERNAME blank in dev to log the link to the console instead of sending.
MAIL_HOST=smtp.mailgun.org      # smtp.eu.mailgun.org for an EU-region Mailgun domain
MAIL_PORT=2525                   # default; see "Outbound SMTP ports" below
MAIL_USERNAME=postmaster@your-domain.mailgun.org
MAIL_PASSWORD=<mailgun-smtp-password>
```

In Docker, `docker-compose.yml` already defines a `postgres` service; set `ACCOUNTS_ENABLED=true`
and `ACCOUNTS_AUTOCONFIG_EXCLUDE=` in the deploy env to turn it on.

### Outbound SMTP ports

`MAIL_PORT` defaults to **2525**, not the usual 587. Many cloud hosts (Scaleway, GCP, …) block
outbound connections on 25 / 465 / 587 to curb spam, and Mailgun listens on **2525** for exactly this
case. A blocked port shows up as the sign-in request hanging, then a server-side
`MailConnectException: Couldn't connect to host … Operation timed out`. If your host *does* allow 587
(or you use a provider that needs it), set `MAIL_PORT=587`. Verify from the host with
`nc -zv <MAIL_HOST> 2525`.

### Gating (why the exclude env var exists)

`game-server` always has the JDBC/Flyway/Postgres jars on the classpath. Spring Boot's
`DataSourceAutoConfiguration` **fails at startup** if no datasource URL is configured, and Spring
Data JDBC probes the DB for its dialect at startup. So by default we exclude
`DataSourceAutoConfiguration` + `FlywayAutoConfiguration` (Flyway and Spring Data JDBC are both
`@ConditionalOnBean(DataSource)`, so they cascade-disable). That keeps the server — and the whole
test/e2e suite — booting with no database.

To enable, **clear** the exclude (`ACCOUNTS_AUTOCONFIG_EXCLUDE=`) so the auto-config can run, and set
`ACCOUNTS_ENABLED=true` so the account beans (`@ConditionalOnProperty`) load and Flyway runs.

## Schema

Flyway migration `V1__init.sql`:

| Table | Purpose |
|-------|---------|
| `users` | account: email (unique), display name, created_at, `is_admin` (added in `V3__admin_role.sql`) |
| `login_tokens` | single-use magic-link tokens (SHA-256 hashed, short TTL) |
| `decks` | saved decks: denormalized name/format + full `SharedDeck` JSON in `data` |
| `match_results` | one row per finished game |
| `match_participants` | a seat in a game (user_id null for guests/AI), won flag |

Flyway migration `V2__match_stats.sql` extends the stats schema:

| Table / columns | Purpose |
|-----------------|---------|
| `match_results.game_mode / frame_count / turn_count` | matchmaking mode + activity measures (the recording gate, games-per-day, mode distribution) |
| `match_participants.colors / set_codes / is_ai / client_ip` | per-seat deck color identity + sets, AI flag (distinguishes AI from guests), and raw client IP (**admin-only**, never sent to clients) |
| `match_participant_cards` | each seat's deck card-by-card (`card_name`, `copies`) — backs most-played-cards + per-card win rate |
| `tournaments` | tournaments + settings (format, mode, set codes, player count, rounds, winner) and a `status` (`IN_PROGRESS` / `COMPLETED` / `ABANDONED`); `ended_at` is null while in progress |
| `tournament_participants` | a seat in a tournament with placement (0 until it finishes) + W/L/D |

Flyway migration `V3__admin_role.sql` adds `users.is_admin` (boolean, default false) — the per-account
admin flag (see **Admin access** below).

Flyway migration `V4__ranked_ratings.sql` adds ranked ELO (see **Ranked play (ELO)** below):

| Table / columns | Purpose |
|-----------------|---------|
| `match_results.ranked` | flags games that adjusted ELO |
| `user_ratings` | current rating per `(user_id, mode)`: `rating`, `games_played`, `wins/losses/draws`, `peak_rating`. Lazily created on first ranked game in a mode; absence = unrated (treated as the starting rating) |
| `rating_history` | one row per ranked game per player: `rating_before/after`, `delta`, `result`, `opponent_user_id`, `opponent_rating`, `game_id` — backs the dashboard's rating-over-time chart |

## Ranked play (ELO)

Signed-in players carry a separate ELO rating in three queues — **Limited**, **Constructed**,
**Commander** (`RankedMode`) — much like MTG Arena splits ranked by format. A game counts as ranked only
when it is **1v1 between two signed-in accounts** (no guests, no AI):

- **Quick games:** a host toggles **Ranked** in the lobby (offered only for a standard 1v1 human-vs-human
  lobby — not AI or Two-Headed Giant). Casual by default.
- **Tournaments:** **ranked by default** for a `TOURNAMENT`-mode bracket (its matches are 1v1); the host
  can uncheck it. Free-for-All / team modes are never ranked.

If a lobby is flagged ranked but a seat isn't a signed-in human at start time, the game still runs — it
just plays **unranked** (the flag is dropped, not blocked). The ranked flag + the queue (`RankedMode`,
derived from the lobby format) are stamped on the `GameSession` at creation, and `GamePlayHandler`
applies the rating change at game-over via `RankedResultSink` — per game, so each game of a best-of-N
match counts.

The math (`ranking/Elo.kt`, pure and unit-tested) is standard ELO calibrated to chess.com-style numbers:
new ratings start at **1200**, an even game between established players shifts about **±10**
(`K = 20`), and a faster **placement** window (`K = 40` for the first 10 games in a mode) lets a new
rating settle quickly. Ratings are uncapped. A display **tier** is derived purely from the rating once
placement is done — Bronze `<1000`, Silver `1000–1199`, Gold `1200–1399`, Platinum `1400–1599`, Diamond
`1600–1999`, **Mythic** `≥2000` (open-ended) — and is shown as **Provisional** during placement. The
profile page shows a card per queue (rating + tier + record) and a rating-over-time line chart.

Flyway migration `V5__game_replays.sql` adds durable replays:

| Table | Purpose |
|-------|---------|
| `game_replays` | one row per finished game keyed by `game_id`: a gzip+base64 `CompactReplay` (RNG seed + decks + ordered action stream) in `data`, plus summary columns. A few KB each — the engine is deterministic, so the whole game is reconstructed from its inputs rather than stored frame-by-frame (see [data-contracts.md](data-contracts.md) → *Compact replays*). |

A signed-in player's history (`/api/stats/me/history`) `LEFT JOIN`s `game_replays` on `game_id` to
flag which past games can be watched/shared (`hasReplay`). Stored replays survive server restarts and
the 100-game in-memory cache; the unguessable `game_id` doubles as the share token via the public
`/replay/{gameId}` page.

Flyway migration `V6__user_uuid.sql` switches the account primary key from a `BIGINT` identity to a
**UUID** (`gen_random_uuid()` default). It backfills every existing row and re-points each foreign key
(`login_tokens`, `decks`, `match_participants`, `tournament_participants`, `user_ratings`,
`rating_history`) — a data-preserving in-place migration, not a recreate. The id is now the shareable
friend code. (Game/replay ids were already UUIDs — `GameSession.sessionId` — so only the account id
changed; the other surrogate keys like `decks.id` stay `BIGINT` since they're internal and never
shared.)

Flyway migration `V7__friends.sql` adds `users.hide_presence` (boolean, default false) and the
`friendships` table:

| Table / columns | Purpose |
|-----------------|---------|
| `friendships` | one directed row per relationship: `requester_id` / `addressee_id` (both → `users(id)`, cascade), `status` (`PENDING` while awaiting the addressee, `ACCEPTED` once mutual), `created_at` / `responded_at`. `CHECK (requester_id <> addressee_id)` + `UNIQUE (requester_id, addressee_id)`. Declining / cancelling / unfriending all delete the row. |
| `users.hide_presence` | per-account presence opt-out — when true the account appears offline to its friends even while connected. |

## Auth flow (magic link)

1. `POST /api/auth/request-login { email }` → upsert account, email a single-use link (logged to
   console if mail isn't configured). Always returns 200 (never reveals whether the email exists).
2. The link is `${APP_BASE_URL}/login/verify?token=…`. The client page exchanges it:
   `POST /api/auth/verify { token }` → `{ authToken, user }`.
3. The client stores `authToken` in `localStorage['argentum-auth']` and sends it as
   `Authorization: Bearer …` on REST calls and on the WebSocket connect handshake.

Auth tokens are stateless HMAC-SHA256-signed (a minimal JWT shape) so REST calls don't hit the DB to
authenticate. The token also links the in-game identity to the account so finished games count toward
the account's stats.

When a signed-in connect handshake carries that token, `ConnectionHandler.linkAccount` stamps the
account id onto the `PlayerIdentity` **and** overwrites its `playerName` with the account's current
profile display name. The server is therefore authoritative over the in-game name for signed-in
players — the client-sent name is only a fallback for guests — so the name set on the profile (and
any later change to it) is what opponents and spectators see. The token itself carries only
`uid`/`email`/`exp`, so the display name is looked up from the `users` table at connect time.

## Admin access

The admin dashboard accepts **two** credentials, resolved by `AdminAuthService`:

1. **Bootstrap password** — the `X-Admin-Password` header matching `GAME_ADMIN_PASSWORD`. Not tied to
   an account; always works (a break-glass path), but meant only to get the first admin in. This also
   works on a server with no database at all (the replay browser is password-only).
2. **Admin account** — a normal `Authorization: Bearer …` whose account has `is_admin = true`. The
   flag is resolved against the DB **per request** (not baked into the token), so a promotion/demotion
   takes effect immediately. Promotion is done from the dashboard's **Players** view.

Bootstrapping the first admin: set `GAME_ADMIN_PASSWORD`, open `/admin`, sign in with the password,
go to **Players**, and promote an account. From then on that account reaches `/admin` with its normal
sign-in and the password can be retired. `GET /api/auth/me` now includes `isAdmin`, which the client
uses to show an **Admin dashboard** link on the profile and to skip the password prompt at `/admin`.

Every admin endpoint (`/api/admin/**` and `/api/stats/admin/**`) accepts either credential.

## Endpoints

| Method | Path | Notes |
|--------|------|-------|
| POST | `/api/auth/request-login` | `{ email }` → 200 |
| POST | `/api/auth/verify` | `{ token }` → `{ authToken, user }` |
| GET | `/api/auth/me` | Bearer → `user` (includes `isAdmin` + `hidePresence`; `id` is the UUID friend code) |
| PUT | `/api/auth/me` | Bearer + `{ displayName }` → updated `user` (1–40 chars; duplicates allowed) |
| GET | `/api/account/decks` | list summaries |
| GET | `/api/account/decks?full` | every deck in full (one round-trip; powers the unified deck browser) |
| GET | `/api/account/decks/{id}` | full deck |
| POST | `/api/account/decks` | body = `SharedDeck` JSON → created deck |
| PUT | `/api/account/decks/{id}` | replace |
| DELETE | `/api/account/decks/{id}` | |

> `/api/account/decks` is intentionally separate from the existing stateless `/api/decks`
> (validation, formats, examples).

### Stats (all under `/api/stats`)

Per-user endpoints take `Authorization: Bearer …`; admin endpoints take either admin credential (see
**Admin access**). Both groups are only mounted when accounts are enabled.

| Method | Path | Notes |
|--------|------|-------|
| GET | `/api/stats/me` | `{ games, wins, losses, winRate }` |
| GET | `/api/stats/me/colors` · `/sets` · `/modes` | `[{ label, count }]` breakdowns |
| GET | `/api/stats/me/opponents` | head-to-head `[{ opponent, isAi, wins, losses }]` |
| GET | `/api/stats/me/history?limit&offset` | recent games |
| GET | `/api/stats/me/cards?limit` | most-played cards |
| GET | `/api/stats/me/tournaments?limit` | tournament finishes with placement |
| GET | `/api/stats/me/ratings` | per-mode ELO `[{ mode, rating, tier, provisional, gamesPlayed, wins, losses, draws, peakRating }]` (all three modes; unrated ones at the starting rating) |
| GET | `/api/stats/me/ratings/history?mode` | rating-over-time points `[{ mode, endedAt, ratingAfter, delta, result }]` (all modes, or one) |
| GET | `/api/stats/admin/overview` | global totals |
| GET | `/api/stats/admin/games-per-day?days` | daily game counts |
| GET | `/api/stats/admin/modes` · `/colors` | global distributions |
| GET | `/api/stats/admin/cards` · `/cards/win-rates?minDecks` | most-played + per-card win rate |
| GET | `/api/stats/admin/tournaments?limit` | recorded tournaments |
| GET | `/api/stats/admin/geo` | IP → coarse location, aggregated by location (raw IPs never returned) |

### Admin — players (`/api/admin/users`, either admin credential)

| Method | Path | Notes |
|--------|------|-------|
| GET | `/api/admin/users` | roster: every account + lifetime games/wins, `isAdmin`, last played |
| GET | `/api/admin/users/{id}` | one account's full stats (overview, colors, modes, head-to-head, top cards, tournaments, recent games) |
| POST | `/api/admin/users/{id}/admin` | `{ isAdmin }` → grant/revoke admin access |

The replay browser lives at `/api/admin/games` and `/api/admin/games/{id}/replay` (also either
credential; password-only on a DB-less server).

Aggregate queries live in `StatsQueryService` (plain SQL via `JdbcTemplate`). Geolocation
(`GeoIpService`) resolves IPs via the free ip-api.com batch endpoint, cached in-process; it's only
called from the admin `geo` endpoint, never the hot recording path.

## Friends & presence

All under `/api/friends` (Bearer; only mounted with accounts enabled). You always act as the token's
account — the body never carries "who am I". You add a friend by their **account id** (the UUID friend
code), so emails stay private.

| Method | Path | Notes |
|--------|------|-------|
| GET | `/api/friends` | accepted friends + each one's live `online` flag |
| GET | `/api/friends/requests` | `{ incoming, outgoing }` pending requests |
| POST | `/api/friends/requests` | `{ accountId }` → send a request. 400 invalid/self · 404 unknown · 409 already-friends / already-requested / they-requested-you |
| POST | `/api/friends/requests/{id}/accept` | addressee accepts |
| DELETE | `/api/friends/requests/{id}` | decline (addressee) or cancel (requester) |
| DELETE | `/api/friends/{accountId}` | unfriend |
| PUT | `/api/friends/visibility` | `{ hidden }` → toggle the presence opt-out |

`FriendsService` owns the request → accept lifecycle (a single directed `friendships` row; symmetric
once `ACCEPTED`). **Presence is derived live, never stored:** `PresenceService` reads the connected
WebSocket identities from `SessionRegistry` — an account is *visibly online* when it holds an open
socket **and** hasn't set `hide_presence`. Updates are pushed in real time: `FriendPresenceBroadcaster`
(hooked into `ConnectionHandler`'s connect/disconnect, plus accept and the visibility toggle) sends
`ServerMessage.FriendPresence` to a user's currently-connected friends, and `FriendRequestReceived`
when a request arrives. These carry no game events, so they need no `ClientEvent.kt` branch. The client
also fetches `/api/friends` on load and on a slow poll as the catch-all for the passive side of an
accept/unfriend.

## Frontend

- `authStore` (standalone Zustand) holds the signed-in user; `api/account.ts` is the REST client.
- Sign-in modal (`LoginModal`) + `/login/verify` page; a nav entry in the connection overlay.
- **Unified Save (everywhere):** every "save a deck" button routes through `useSaveDeck` — when signed
  in it saves to the account (cloud, overwriting a same-named deck via `upsertDeckByName`), otherwise
  to the browser library (localStorage). This covers the deckbuilder Save/Save-as, the tournament/draft
  "Save Deck" + deck-viewer save (previously always localStorage, even when signed in), and the lobby
  deck picker's Paste-tab save.
- **Unified deck library (`useUnifiedDecks`):** merges account decks (fetched in full via `?full`) with
  browser-only decks, each tagged `online`. Feeds (a) the deckbuilder's saved-deck **browser** overlay,
  which shows an **Online / Browser** badge per deck and routes load/rename/delete to the right store,
  and (b) the lobby deck picker's "My Decks" tab, so signed-in users can pick their cloud decks to play.
- **Display name:** editable on the profile page (`PUT /api/auth/me`); the email stays the identity.
- Profile page at `/profile` shows the win/loss summary plus colors played (a Recharts bar chart),
  sets, game modes, head-to-head, most-played cards, tournament finishes, and a recent-games list — all
  from `/api/stats/me/*` via `api/account.ts`. Each recent game with a stored replay (`hasReplay`)
  gets **Watch** (opens the public `/replay/{gameId}` viewer) and **Share** (copies that link)
  buttons, so a signed-in player can rewatch and share their own past games. It also has a small
  **Manage my decks** launcher that opens the deckbuilder's deck browser (`/deckbuilder?decks=open`).
- Admin page at `/admin` is a **hub** (`AdminPage` → `AdminHub`) that routes to three areas, each its
  own self-scrolling screen (`AdminScreen` in `adminUi.tsx` — the whole app runs in
  `#root { overflow: hidden }`, so admin screens scroll themselves rather than the document):
  - **Stats** (`AdminDashboard`, `api/adminStats.ts`) — headline totals, games-per-day line chart,
    mode/color distributions, most-played + highest-win-rate cards, recorded tournaments, geolocation.
  - **Replays** (`ReplayViewer`) — browse and play back every completed game.
  - **Players** (`AdminPlayers`, `api/adminUsers.ts`) — the account roster, a per-account drill-down
    (full stats + recent games), and the **Make admin / Revoke admin** control.
  Admin auth is the shared `AdminAuth` (`api/adminAuth.ts`): the bootstrap password (kept in
  sessionStorage) or, for an admin account, its Bearer token. A signed-in admin skips the password
  prompt entirely; the profile page shows an **Admin dashboard** link when `user.isAdmin`.
- On sign-in, a landing-page prompt (`DeckMigrationPrompt`) offers to copy browser-only decks to the
  account.
- **Friends** (`/friends`, `pages/FriendsPage.tsx`, `api/friends.ts`, standalone `friendsStore`): your
  friend code with a copy button, an add-by-code box, incoming/outgoing requests, the friends list with
  a green/grey online dot + unfriend, and the **Hide my online status** toggle. `AuthWidget` carries a
  **Friends** nav link with an incoming-request count badge; the `ProfilePage` header links to it too.
  Live `friendPresence` / `friendRequestReceived` pushes are routed (via the WebSocket message handlers)
  into `friendsStore`; the store also loads on sign-in and on a slow poll while the page is open.

## Tests

- `AuthTokenServiceTest` — token sign/verify/expiry/tamper (pure unit; `uid` is now a UUID string).
- `MagicLinkServiceTest` — login/verify orchestration with mocked repositories.
- `FriendsServiceTest` — the request/accept/decline/cancel/unfriend rules (self / unknown / duplicate /
  reverse-pending), and that `listFriends` online status respects `hide_presence` (mocked repos +
  presence).
- `FlywayMigrationTest` — applies the migrations against a real Postgres via Testcontainers and
  exercises the account/deck/stats round-trip, the V2 stats schema and its Postgres-specific aggregate
  SQL (set-code unnest, card win-rate `FILTER`, games-per-day interval, tournament round-trip, cascade
  deletes), the **V4 BIGINT→UUID swap preserving rows + foreign keys** (seed at `target("3")`, migrate,
  assert data + FK integrity), and the **V5 friends** request/accept round-trip + cascade. Self-skips
  when Docker is unavailable.
- `DeckProfilerTest` — deck color-identity (WUBRG order) + set derivation, colorless/fallback/pin cases.
- `MatchResultSinkTest` — the recording guard: AI-only games skipped, human/guest games recorded with
  their deck cards; same for the tournament sink.
