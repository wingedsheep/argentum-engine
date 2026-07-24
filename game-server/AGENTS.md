# game-server

Thin Spring Boot orchestration layer around `rules-engine`. WebSocket routing, session management,
state masking, DTO transformation, tournament lobbies. **No game logic** — that lives in the engine.

## Build & Test

```bash
just test-server
./gradlew :game-server:bootRun     # Start server
./gradlew :game-server:test
```

Game-server has almost no unit tests. Behavior is verified through E2E tests in `e2e-scenarios/`.

## Where to look

- **Engine ↔ API contract** (action dispatch, state updates, masking): [`../docs/engine-server-interface.md`](../docs/engine-server-interface.md)
- **Client/server JSON payloads** (`StateUpdate`, `ServerMessage`, `ClientGameState`):
  [`../docs/data-contracts.md`](../docs/data-contracts.md)
- **Async I/O and decision protocol**: [`../docs/player-input.md`](../docs/player-input.md)
- Handler classes under `src/main/kotlin/.../handlers/` — read directly.

## Load-bearing rules

- **All `GameSession` state mutations must hold `synchronized(stateLock)`.** Idempotency is enforced
  via `lastProcessedMessageId`; retransmits return `Failure("Duplicate message")` without re-execution.
- **Use projected state, not base components**, anywhere ownership/control matters (mana sources,
  legal actions, card transformation). `ControllerComponent` alone misses control-changing effects.
- **`GameWebSocketHandler` wires callbacks in `@PostConstruct`** to break the
  `GamePlayHandler ↔ LobbyHandler` cycle — don't try to constructor-inject these.
- **Adding a `ServerMessage` type** also requires an exhaustive `when` branch in `ClientEvent.kt` if
  it carries game events. `ClientMessage.kt` for client→server, plus a `handleMessage()` branch.
- **Tournament dynamic matchmaking** means future-round opponents aren't deterministic; only send
  `nextOpponentName` for the *current* round. Per-lobby `roundLocks` guard round advancement.
