# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Role of game-server

The game-server is a **thin orchestration layer** around the rules engine. It does not contain game logic — that lives in `rules-engine`. This module handles WebSocket routing, session management, state masking, and DTO transformation.

## Build & Test

```bash
just test-server                          # Run game-server tests
./gradlew :game-server:bootRun            # Start server
./gradlew :game-server:test               # Run game-server tests via Gradle
```

The game-server has almost no unit tests. All behavior is verified through E2E tests in `e2e-scenarios/` using Playwright against the running server + client. See the root CLAUDE.md for E2E test patterns.

## Architecture

### Message Flow

```
Client WebSocket → GameWebSocketHandler → ConnectionHandler / GamePlayHandler / LobbyHandler
                                              ↓
                                         GameSession (synchronized)
                                              ↓
                                    ActionProcessor (rules-engine)
                                              ↓
                                    GameState (immutable)
                                              ↓
                              ClientStateTransformer → ClientGameState DTO → StateUpdate message
```

### Key Classes

| Class | Purpose |
|-------|---------|
| `GameWebSocketHandler` | Entry point — routes JSON messages to handlers |
| `GameSession` | Per-game state container; wraps engine with thread safety |
| `PlayerSession` | Per-player WebSocket + identity |
| `ClientStateTransformer` | Transforms `GameState → ClientGameState` DTO with state masking |
| `DevScenarioController` | REST endpoint for E2E scenario setup (`POST /api/dev/scenarios`) |
| `AutoPassManager` | Arena-style priority auto-pass logic |
| `LegalActionsCalculator` | Computes legal actions per player from current state |

### GameSession threading

`GameSession` uses `synchronized(stateLock)` to protect `gameState`. All state mutations must acquire this lock. `lastProcessedMessageId: ConcurrentHashMap<EntityId, String>` provides idempotency — if a client retransmits the same `messageId`, the server returns `Failure("Duplicate message")` without re-executing.

### Circular dependency pattern

`GameWebSocketHandler` uses a **callback injection pattern** (wired in `@PostConstruct wireCallbacks()`) to break circular dependencies:
- `GamePlayHandler` needs to notify `LobbyHandler` when a game ends
- `LobbyHandler` needs to notify `GamePlayHandler` when a new round starts
- These callbacks are injected after construction, not passed through constructors

### State masking

`ClientStateTransformer` performs masking inline while building DTOs — `StateMasker.kt` exists but is not used in the main DTO path. Masking rules:
- **Library**: always hidden (only size shown)
- **Hand**: only owner sees cards; opponent sees count only
- `RevealedToComponent` overrides hiding for specific entities

### Projected state vs. base state

**Critical:** Multiple places must use `StateProjector.project()` (projected state) rather than reading `ControllerComponent` directly. Code that determines card ownership, legal actions, or mana availability must use projected state to correctly handle control-changing effects. The files that do this correctly are `ClientStateTransformer.transformCard()`, `ManaSolver`, `GameSession.getLegalActions()`, and `ActivateAbilityHandler`.

## Dev Scenario API

`DevScenarioController` (`POST /api/dev/scenarios`) enables E2E test setup:

1. Receives a `ScenarioRequest` JSON (cards in specific zones, phase, active player)
2. `ScenarioBuilder` constructs engine `GameState` directly
3. Calls `gameSession.injectStateForDevScenario(state)` — no players connected yet
4. Pre-registers player identities in `SessionRegistry`
5. Returns tokens; E2E test clients connect via WebSocket using these tokens

Enabled by `game.dev-endpoints.enabled=true` in `application.properties`.

## StateUpdate structure

Every action sends a `ServerMessage.StateUpdate` to both players containing:
- `state: ClientGameState` — masked board state from their perspective
- `events: List<ClientEvent>` — filtered game log (taps/untaps/mana additions excluded)
- `legalActions: List<GameAction>` — all legal moves for the priority player
- `pendingDecision: PendingDecision?` — active player input request (choose targets, scry, etc.)
- `nextStopPoint: StopPoint?` — informs client when auto-pass will next stop
- `opponentDecisionStatus: OpponentDecisionStatus?` — masked summary of what opponent is deciding

## Auto-pass

`AutoPassManager.shouldAutoPass(playerId, gameState, legalActions)`:
- Returns `true` if the only legal action is Pass and no stop overrides apply
- Full-control mode (`SetFullControl(true)`) disables all auto-passing
- Per-step stops via `SetStopOverrides` — granular control per player per step
- Non-battlefield activated abilities always stop even if auto-pass would normally apply

## Tournament System

### Lobby Lifecycle

`TournamentLobby` manages a state machine:

```
WAITING_FOR_PLAYERS → [Sealed] DECK_BUILDING → TOURNAMENT_ACTIVE → TOURNAMENT_COMPLETE
WAITING_FOR_PLAYERS → [Draft]  DRAFTING → DECK_BUILDING → TOURNAMENT_ACTIVE → TOURNAMENT_COMPLETE
```

Formats: `SEALED` (open boosters, build deck) and `DRAFT` (pick cards from rotating packs, then build deck).

### Key Classes

| Class | Purpose |
|-------|---------|
| `TournamentLobby` | Lobby state machine, player tracking, ready state, card pools |
| `TournamentManager` | Round-robin schedule generation, match tracking, standings |
| `LobbyHandler` | All tournament handler methods (ready, match start/complete, round complete) |
| `LobbyPlayerState` | Per-player state: card pool, submitted deck, current draft pack |

### Scheduling

`TournamentManager` generates a full **round-robin** schedule upfront using the circle method:
- N-1 rounds for N players (padded to even), repeated `gamesPerMatch` times
- Odd player counts get a BYE (null `player2Id`) per round
- BYE matches auto-complete immediately when a round starts

### Dynamic Matchmaking (Eager Match Starting)

Matches start as soon as both players are ready — no waiting for the entire round:

1. Player sends `ReadyForNextRound` → `handleReadyForNextRound()`
2. Per-lobby lock prevents concurrent round advancement races
3. Epoch-based stale request detection discards ready requests that arrived during round transitions
4. `tryStartMatchForPlayer()` checks if this player's next opponent is also ready
5. If both ready → `startSingleMatch()` immediately
6. Cross-round safety: prevents "stealing" an opponent from a current-round match by checking if the opponent has completed their current-round match first

**Important:** Because of dynamic matchmaking, future-round opponents are **not guaranteed** — the actual opponent depends on which games finish first. The server only sends `nextOpponentName` when the match is in the **current round**. For future-round matches, the client shows "Waiting for matchup..." instead.

### Match & Round Flow

```
Player Ready → tryStartMatchForPlayer() → Both ready? → startSingleMatch()
                                                           ↓
                                                   TournamentMatchStarting (to both)
                                                           ↓
                                                   Game plays out via GameSession
                                                           ↓
                                                   handleMatchComplete()
                                                   → MatchComplete (to match players)
                                                   → tryStartMatchForPlayer() for waiting players
                                                           ↓
                                                   All round matches done?
                                                   → handleRoundComplete()
                                                   → RoundComplete (to all)
                                                           ↓
                                                   All rounds done?
                                                   → completeTournament()
```

### BYE Handling

- BYE matches (`player2Id == null`) auto-complete in `startNextRound()`
- `TournamentBye` message sent to BYE player; they can spectate other matches
- Auto-ready on the client: when a player has a BYE between rounds (`nextRoundHasBye`), the frontend auto-sends `ReadyForNextRound` (except before round 1 to allow deck editing)
- `tryStartMatchForPlayer()` handles BYEs recursively — if a BYE is found, it completes it and looks for the next real match

### Server Messages (Tournament)

| Message | When | Key Fields |
|---------|------|------------|
| `TournamentStarted` | Tournament begins or reconnect | `nextOpponentName`, `nextRoundHasBye`, `totalRounds` |
| `TournamentBye` | Player has BYE this round | `round` |
| `TournamentMatchStarting` | Match confirmed and starting | `gameSessionId`, `opponentName` |
| `MatchComplete` | A match finishes | `nextOpponentName`, `nextRoundHasBye`, `standings` |
| `RoundComplete` | All matches in round finish | `nextOpponentName`, `nextRoundHasBye`, `standings` |
| `PlayerReadyForRound` | A player readies up | `readyPlayerIds`, `totalConnectedPlayers` |

### Reconnection

`sendTournamentStatusToPlayer()` handles 4 cases when a player reconnects mid-tournament:
1. **No current round** — send `TournamentStarted` (waiting for ready)
2. **Active game in progress** — send `TournamentMatchStarting` + rejoin the `GameSession`
3. **BYE this round** — send `TournamentBye` + restore spectating if applicable
4. **Match complete / waiting** — send `MatchComplete` with standings + ready status

### Concurrency

- **Per-lobby lock** (`roundLocks`): prevents two players from simultaneously advancing rounds
- **Ready epoch** (`lobby.readyEpoch`): incremented on round complete; stale ready requests (captured before lock, changed after) are discarded
- **`clearPlayerReady()`**: clears ready state per-player when their match starts

## Adding a new server message type

1. Add entry to `ServerMessage.kt` (sealed interface)
2. Add entry to `ClientMessage.kt` if it's client → server
3. Add a handler branch in `GameWebSocketHandler.handleMessage()` for client messages
4. If the message produces game events, add a branch to `ClientEvent.kt` (exhaustive `when`)
