# Game Server Architecture Improvements

The game-server module (~19k lines, 54 files) is a functional orchestration layer with clean separation from the rules engine. However, several structural issues make it harder to extend, test, and maintain than necessary. This document proposes improvements grouped by priority, focusing on modularity, clarity, and maintainability.

See also: `game-server-scalability.md` for performance-specific concerns.

## HIGH — Significant Impact on Maintainability

### 1. Break up GameSession god class (1,011 lines)

`GameSession.kt` is the largest and most coupled class in the module. It handles game state management, mulligan orchestration, auto-pass checkpoint tracking, priority mode management, stop overrides, replay snapshot recording, game log accumulation, spectator state building, legal action queries, state transformation/delta computation, and test state injection.

**Proposed extractions:**

| New Class | Responsibility | Lines saved |
|-----------|---------------|-------------|
| `PriorityModeManager` | Priority modes, stop overrides, full-control toggle | ~80 |
| `UndoManager` | Checkpoint creation, undo eligibility predicates, restore logic | ~100 |
| `ReplayRecorder` | Snapshot accumulation, spectator state building | ~60 |
| `GameLogAccumulator` | Per-player event log filtering and storage | ~40 |

`GameSession` becomes a thin coordinator delegating to these focused classes. Each extraction is independently testable.

### 2. Replace callback injection with Spring events

`ConnectionHandler`, `GamePlayHandler`, and `LobbyHandler` are wired together via 8+ mutable callback fields:

- `handleGameOverCallback`
- `broadcastStateUpdateCallback`
- `handleAbandonCallback`
- `joinSealedGameCallback`
- `joinLobbyCallback`
- `handleReadyForNextRoundCallback`
- plus others

This creates circular initialization order, makes the dependency graph invisible, and prevents adding new handlers without modifying existing ones.

**Proposed approach:** Use Spring's `ApplicationEventPublisher` / `@EventListener`:

```kotlin
// Publish
eventPublisher.publishEvent(GameOverEvent(gameId, winnerId))

// Subscribe (in any @Component)
@EventListener
fun onGameOver(event: GameOverEvent) { ... }
```

Events to introduce: `GameOverEvent`, `StateUpdatedEvent`, `PlayerDisconnectedEvent`, `PlayerReconnectedEvent`, `TournamentRoundCompletedEvent`, `PlayerAbandonedEvent`.

### 3. Break up ConnectionHandler (519 lines)

`ConnectionHandler` handles connection lifecycle, reconnection with grace periods, tournament disconnect broadcasts, player identity management, and lobby state notifications.

**Proposed extractions:**

| New Class | Responsibility |
|-----------|---------------|
| `DisconnectTimerManager` | Grace period scheduling, timer cancellation, timeout handling |
| `ReconnectionOrchestrator` | Context determination (lobby/game/tournament), state resend |

`ConnectionHandler` keeps only WebSocket connect/disconnect entry points.

### 4. Break up GamePlayHandler (696 lines)

`GamePlayHandler` mixes game creation/joining, action submission, mulligan flow, game-over handling with replay saving, auto-pass loop execution, and priority/stop configuration.

**Proposed extractions:**

| New Class | Responsibility |
|-----------|---------------|
| `GameCreationHandler` | Create/join game, waiting session management |
| `AutoPassProcessor` | Auto-pass loop execution after state mutations |
| `GameOverHandler` | Game completion, replay saving, tournament result reporting |

### 5. Split LobbyHandler by feature

`LobbyHandler` handles three distinct features in one class: sealed pool management, tournament lobby lifecycle, and draft picking (standard, Winston, grid).

**Proposed split:**

| New Class | Responsibility |
|-----------|---------------|
| `SealedPoolHandler` | Sealed game creation, pool generation, deck submission |
| `TournamentHandler` | Tournament lobby, round advancement, match pairing |
| `DraftHandler` | Draft picking across all three variants |

### 6. Formalize PlayerIdentity state machine

`PlayerIdentity` has 7 mutable `@Volatile` fields with implicit state transitions inferred from null checks. Adding a new connection state requires auditing every null check across the codebase.

**Proposed approach:**

```kotlin
sealed interface ConnectionState {
    data class Connected(val session: WebSocketSession) : ConnectionState
    data class Disconnected(val expiresAt: Long, val timer: ScheduledFuture<*>) : ConnectionState
    data object Expired : ConnectionState
}

sealed interface GameState {
    data object InLobby : GameState
    data class InGame(val gameSessionId: String) : GameState
    data class Spectating(val gameSessionId: String) : GameState
}
```

State transitions become explicit and exhaustive `when` blocks catch missing cases at compile time.

## MEDIUM — Worth Addressing

### 7. Fix thread safety gaps

Several mutable collections in `GameSession` are not thread-safe despite being accessed under concurrent WebSocket messages:

- `spectators: MutableMap` (should be `ConcurrentHashMap`)
- `deckLists: MutableMap` (should be `ConcurrentHashMap`)
- `gameLogs: MutableMap` of `MutableList` (race on list append)

`ConnectionHandler` iterates `lobby.players` during broadcasts without synchronization, risking `ConcurrentModificationException`.

Fix: Use concurrent collections or ensure all access is under the existing `stateLock`.

### 8. Standardize error handling strategy

Current error handling is inconsistent:
- Some paths call `sender.sendError()` and return (silent to server)
- Some paths throw exceptions caught by `GameWebSocketHandler` catch-all
- Some paths log and swallow exceptions (e.g., `broadcastStateUpdate()`)
- No structured error types beyond `ErrorCode` enum

**Proposed approach:**
- Define a `GameServerException` hierarchy with error codes
- Handlers return `Result<T>` for fallible operations
- Central exception handler in `GameWebSocketHandler` maps to `ServerMessage.Error`
- Never swallow exceptions in state-mutation paths

### 9. Improve unit test coverage

The game-server has almost no isolated unit tests. All behavior is verified through E2E Playwright tests, which are slow and don't pinpoint failures. The god classes make unit testing difficult (circular dependency).

After the extractions in items 1-5, each new class should have focused unit tests:
- `PriorityModeManager`: test mode transitions
- `UndoManager`: test checkpoint eligibility
- `AutoPassProcessor`: test pass decisions with mocked state
- `DisconnectTimerManager`: test grace period logic
- `ReconnectionOrchestrator`: test context determination

### 10. Deduplicate state masking

State masking logic exists in two places:
- Inline in `ClientStateTransformer.transform()` (used in production)
- `StateMasker` class (separate implementation)

Pick one approach and remove the other. If `StateMasker` is the intended abstraction, integrate it into `ClientStateTransformer`. If inline masking is preferred, delete `StateMasker`.

### 11. Clean up protocol message types

`ServerMessage.kt` and `ClientMessage.kt` use string-based type discrimination with manual JSON parsing. Consider using kotlinx.serialization polymorphic serialization with `@SerialName` discriminators for type-safe message routing instead of `when (type)` string matching in `GameWebSocketHandler`.

## LOW — Nice to Have

### 12. Extract auto-pass rules into data-driven configuration

`AutoPassManager` (680 lines) has hardcoded step predicates for 4 rules of Arena-style auto-passing. Adding a new stop condition requires updating multiple methods.

**Proposed:** Define rules as a list of `AutoPassRule(predicate, description)` objects that are evaluated in order. Each rule is independently testable and new rules can be added without modifying existing ones.

### 13. Add structured logging with context

Current logging uses plain `logger.info/debug/error` without consistent context. Add:
- Game session ID to all game-related logs
- Player identity to all player-related logs
- Use MDC (Mapped Diagnostic Context) for automatic inclusion

### 14. Consider coroutine-based concurrency

The current model uses `synchronized` blocks and `ScheduledExecutorService`. Kotlin coroutines with `Mutex` and structured concurrency would be more idiomatic, composable, and testable. This is a larger refactor best done after the class extractions.

### 15. Consolidate configuration classes

5 config files (`GameProperties`, `WebSocketConfig`, `SwaggerConfig`, `GameBeansConfig`, `CacheConfig`) could be simplified. `GameBeansConfig` registers card sets imperatively — consider auto-discovery via `ServiceLoader` (already exists in rules-engine but unused by game-server).

## Suggested Order of Execution

1. **Event bus** (item 2) — unblocks handler splits by removing callback wiring
2. **Split GameSession** (item 1) — biggest single improvement
3. **Split handlers** (items 3-5) — now possible without callback tangles
4. **PlayerIdentity state machine** (item 6) — cleans up reconnection logic
5. **Thread safety fixes** (item 7) — quick wins after structure is clearer
6. **Error handling** (item 8) — standardize across new smaller classes
7. **Unit tests** (item 9) — write tests for each new extracted class
8. **Remaining items** — as time permits
