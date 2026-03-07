# Game Server Scalability

The server is well-structured for its current use case (small number of concurrent 2-player games). It uses immutable GameState, per-session locking, ConcurrentHashMap-backed registries, and optional Redis persistence. However, several patterns would become bottlenecks at scale.

## HIGH — Address Before Scaling

### 1. Full state sent on every action (no delta updates)
Every player action triggers a full `ClientGameState` serialization — all zones, all entities, all legal actions. With large boards (50+ permanents) and thousands of cards in the pool, this becomes significant bandwidth and CPU overhead. Delta/diff-based updates would dramatically reduce payload size.

### 2. JSON serialization under per-session lock (`MessageSender.kt:32`)
`json.encodeToString(message)` happens inside `synchronized(lock)`, meaning a large state update blocks all other writes to that player's session. Serialization should happen outside the lock, with only the `session.sendMessage()` call synchronized.

### 3. Legal actions recalculated from scratch every update (`LegalActionsCalculator`)
Called twice per action (once per player) with no caching. Complexity is O(hand × mana sources × battlefield). For boards with many permanents and activated abilities, this becomes expensive. Since `GameState` is immutable, legal actions could be lazily cached per state instance (like `projectedState` already is).

### 4. State projection called twice per action (`ClientStateTransformer.kt:61`)
`state.projectedState` is lazily cached on the `GameState` object, so both player transforms share the same projection. However, `ClientStateTransformer.transform()` itself iterates all entities per player — two full traversals. A single traversal producing both masked views would halve the work.

### 5. Unbounded replay snapshot accumulation (`GameSession.kt:94`)
`replaySnapshots: CopyOnWriteArrayList<SpectatorStateUpdate>` stores a full serialized state snapshot per action for the entire game. Long games (100+ actions) accumulate significant memory. Consider streaming snapshots to storage or capping in-memory retention.

### 6. Unbounded game log accumulation (`GameSession.kt:87`)
`gameLogs` appends filtered events per player with no cap. Same growth concern as replay snapshots.

## MEDIUM — Worth Addressing

### 7. No server thread pool configuration (`application.yml`)
Missing `server.tomcat.threads.max`, `server.tomcat.max-connections`, compression settings. Defaults (200 threads) are fine for small scale but should be explicit. Java 21 virtual threads (`spring.threads.virtual.enabled: true`) would be a low-effort win for WebSocket-heavy workloads.

### 8. Session lock map never pruned (`SessionRegistry.kt:79`)
`sessionLocks: ConcurrentHashMap` creates a lock per WebSocket session ID but only removes it on explicit disconnect. Long-running servers accumulate dead entries. Similarly, `LobbyHandler.roundLocks` (line 53) are never cleaned up after tournaments complete.

### 9. Disconnect scheduler is fixed at 2 threads (`SessionRegistry.kt:30`)
`Executors.newScheduledThreadPool(2)` handles all disconnect grace period timers. With hundreds of concurrent disconnections (e.g., network blip during a tournament), the scheduler could become a bottleneck.

### 10. Redis cache divergence
`RedisGameRepository` and `RedisLobbyRepository` maintain in-memory `ConcurrentHashMap` caches alongside Redis, but don't invalidate entries when Redis TTL expires. Stale cached data could be served after Redis expiry.

### 11. Card registry is hard-coded (`GameBeansConfig.kt:20-35`)
Sets are registered via Spring config flags. Adding new sets requires code changes + restart. The `SetLoader` with `ServiceLoader` exists in rules-engine but isn't used by game-server. For thousands of cards, dynamic/hot-reloadable card loading would be valuable.

## LOW — Fine for Now

### 12. Single stateLock per GameSession (`GameSession.kt:50`)
Serializes all actions within a game. This is actually correct for MTG (actions are inherently sequential due to priority), so it's not a real bottleneck — just a note that games can't process actions in parallel.

### 13. StateProjector O(n²) dependency resolution
Trial application checks all effect pairs within each layer. Rare in practice (few effects share a layer with dependencies), but could matter with boards full of type-changing and lord effects.

### 14. `forEachIdentity()` linear scan (`SessionRegistry.kt:48`)
Called during disconnect time extension. O(n) in total connected players. Acceptable unless player count reaches tens of thousands.

## Recommended Priority Actions

1. **Move JSON serialization outside the session lock** — simplest fix, biggest immediate win
2. **Cache legal actions on GameState** (lazy property like `projectedState`) — eliminates redundant computation
3. **Cap replay snapshots and game logs** — either stream to storage or ring-buffer the last N entries
4. **Enable virtual threads** — single config line, improves WebSocket concurrency significantly
5. **Add explicit thread pool and connection limit config** — operational hygiene
6. **Clean up lock maps on session/tournament close** — prevents slow memory leak
7. **Consider delta state updates** — most impactful for bandwidth, but highest implementation effort
