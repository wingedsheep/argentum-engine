# League & Season System

A persistent multiplayer league system where friends create private leagues, play matches over time, and track
standings across seasons. Requires a proper account system (SSO via Keycloak) to support persistent player identity.

## Dependencies

This feature depends on:
- **Account system** (Phase 1 below) — persistent player identity is a prerequisite for everything else
- PlayerIdentity state machine formalization ([architecture #6](archived/game-server-architecture.md#6-formalize-playeridentity-state-machine)) — current identity model is ephemeral and needs rework

---

## Phase 1 — Accounts & Authentication

**Superseded by [google-oauth2-accounts.md](google-oauth2-accounts.md).** That document is the
authoritative plan for the account system. It drops Keycloak in favour of direct Spring Security +
Google OAuth2, adds Postgres + Flyway, defines the `UserAccount` entity and JIT provisioning, and
specifies how the session cookie bridges into the WebSocket handshake while preserving guest mode.

---

## Phase 2 — Leagues

A league is a private group of players who play matches against each other over time.

### 2.1 Data Model

```
League {
    id: UUID
    name: String
    description: String?
    createdBy: UUID              // UserAccount ID
    inviteCode: String           // short code for sharing (e.g., "BOLT-42")
    createdAt: Instant
    settings: LeagueSettings
}

LeagueSettings {
    maxMembers: Int              // default 8
    format: LeagueFormat         // CONSTRUCTED, SEALED, DRAFT
    deckRules: DeckRules?        // optional: allowed sets, ban list
}

LeagueMembership {
    leagueId: UUID
    userId: UUID
    role: MemberRole             // OWNER, MEMBER
    joinedAt: Instant
}
```

### 2.2 League Management

- **Create league** — owner sets name, format, max members
- **Invite link** — shareable URL with invite code (e.g., `/join/BOLT-42`)
- **Join league** — click invite link while logged in
- **Leave league** — member can leave; owner can kick members
- **League home page** — member list, current season standings, match history

### 2.3 Matchmaking Within a League

- **Challenge system** — any member can challenge another to a match
- Challenge appears as notification for the other player
- Both players must be online to play (no async)
- Match results automatically recorded to the league
- Optional: round-robin scheduler suggests who should play next (balances matchups)

### 2.4 API Endpoints

```
POST   /api/leagues                     Create league
GET    /api/leagues                     List my leagues
GET    /api/leagues/{id}                League details + members
POST   /api/leagues/{id}/join           Join via invite code
DELETE /api/leagues/{id}/members/{uid}  Leave / kick
POST   /api/leagues/{id}/challenges     Challenge a member
GET    /api/leagues/{id}/challenges     Pending challenges
POST   /api/leagues/{id}/challenges/{cid}/accept
```

---

## Phase 3 — Seasons & Standings

A season is a time-boxed competition within a league. Leagues can run multiple seasons.

### 3.1 Data Model

```
Season {
    id: UUID
    leagueId: UUID
    name: String                 // e.g., "Spring 2026"
    startDate: LocalDate
    endDate: LocalDate
    status: SeasonStatus         // UPCOMING, ACTIVE, COMPLETED
    scoringSystem: ScoringSystem // MATCH_WINS, ELO, SWISS_POINTS
}

SeasonStanding {
    seasonId: UUID
    userId: UUID
    matchesPlayed: Int
    matchWins: Int
    matchLosses: Int
    gameWins: Int
    gameLosses: Int
    eloRating: Int               // default 1200
    points: Int                  // computed from scoring system
}

MatchRecord {
    id: UUID
    seasonId: UUID
    player1Id: UUID
    player2Id: UUID
    player1GameWins: Int
    player2GameWins: Int
    winnerId: UUID?
    format: String               // BEST_OF_1, BEST_OF_3
    playedAt: Instant
    gameSessionIds: List<String> // link to actual game sessions for replay
}
```

### 3.2 Scoring Systems

- **Match Wins** — simple W/L record, sorted by win percentage
- **ELO** — Elo rating (K=32 for new players, K=16 for established), better for uneven match counts
- **Swiss Points** — 3 for match win, 1 for draw, 0 for loss; tiebreakers: opponent match-win %, game-win %

### 3.3 Season Lifecycle

1. **Owner creates season** — sets dates, scoring system, format
2. **Season starts** — members play matches at their own pace
3. **Standings update live** — leaderboard recalculated after each match
4. **Season ends** — final standings frozen, champion crowned
5. **New season** — owner creates next season, ratings can carry over or reset

### 3.4 Standings Page

- Leaderboard table: rank, player, W-L, win%, ELO, games played
- Match history: recent results with links to replays
- Head-to-head records between members
- Personal stats: best win streak, most-played colors, favorite cards

---

## Phase 4 — Social & Polish

### 4.1 Notifications

- In-app notifications for: challenges received, match results, season start/end
- Optional: email digest (weekly standings summary)
- WebSocket push for real-time notifications when online

### 4.2 Match History & Replays

- Link match records to game session replay data
- "Watch replay" button on match history
- Replay viewer: step through game actions with board state visualization

### 4.3 Player Profiles

- Public profile page: leagues, season records, all-time stats
- Match history across all leagues
- Achievements (optional, stretch): "First blood", "10-win streak", "Play 100 games"

### 4.4 League Activity Feed

- Timeline of recent events: matches played, members joined, season milestones
- Chat / message board per league (stretch goal)

---

## Technical Considerations

### Infrastructure Additions

| Component    | Purpose                              | Dev Setup          | Prod Setup                   |
|--------------|--------------------------------------|--------------------|------------------------------|
| **Keycloak** | SSO, Google login, user management   | Docker Compose     | Managed (e.g., Cloud IAM)   |
| **Postgres** | User accounts, leagues, standings    | Docker Compose     | Managed Postgres             |
| **Redis**    | Already used for game state caching  | Already configured | Already configured           |

### Migration Strategy

The current ephemeral `PlayerIdentity` system must coexist with accounts during transition:
1. Add Keycloak + login flow (Phase 1)
2. Authenticated users get persistent identity; anonymous users keep ephemeral tokens
3. League features require login; casual play remains open
4. Eventually: link past tournament results to accounts (optional, manual claim)

### Security

- Keycloak handles password storage, OAuth flows, token refresh — no custom auth code
- Server validates JWTs on every WebSocket connection and REST call
- PKCE flow for the SPA (no client secret in browser)
- Rate limiting on league creation, challenges, invites
- Invite codes: short-lived or revocable by owner

### Scaling Notes

- Leagues and seasons are low-write, high-read — Postgres with simple indexing is fine
- ELO recalculation is O(1) per match — no batch recomputation needed
- Standings can be cached in Redis with short TTL for the leaderboard page
- Match records link to existing game session IDs — no duplication of game state
