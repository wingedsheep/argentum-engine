# Gym JSON API & Gymnasium Wrapper

Expose the existing Kotlin self-play gym (`rules-engine/.../gym/`) as a JSON-driven training environment with a Python Gymnasium wrapper, performant parallel rollouts, and a clear path to MCTS / AlphaZero-style usage. Enables external RL researchers (e.g., MageZero) to train on the engine and test their own decks.

---

## Architectural stance

1. **Stable per-step action IDs, not reified action JSON.** Engine hands each legal action a deterministic integer index within the current step. Trainers post the integer back. Trivial action masking, card-set-agnostic wire format, cheap Python validation.
2. **New `gym-server` Gradle module, not an extension of `game-server`.** Spring MVC lobbies/WebSocket/tournament/UI-DTO weight is dead weight in the hot loop. `gym-server` shares `rules-engine` only; runs on a separate port.
3. **`PendingDecision` options share the action-ID space.** One `Discrete(N)` policy head, one mask. `pendingDecision` appears as a separate *observation* field so the policy knows it's a scry/target/choose-X moment, but the chosen option still comes back as `actionId: int`.
4. **Opponent hand hidden by default.** Perspective-player info-set masking like a real game. `revealAll: Boolean` flag on `GameConfig`, off by default, only honored when `game.dev-endpoints.enabled=true`.
5. **Constructed decks are first-class.** Deck submission accepts either `{sealedSetCode: "BLB"}` or `{cards: [{name, count}]}` — same `Map<String, Int>` shape `SealedDeckGenerator` already produces.
6. **Versioning: schema hash + server build version.** `/v1/spec` returns `{schemaHash, serverVersion, schema}`. Python client caches the hash, warns on mismatch, hard-fails if `strict=True`. No SemVer on the schema pre-1.0.
7. **Colocated Python package** at `python/argentum_gym/` until there's an external user; then consider splitting for pip.

---

## Phase 1 — Training observation/action contract (`rules-engine`)

**Deliverable:** `rules-engine/.../gym/contract/` subpackage — serializer-friendly Kotlin records. No HTTP yet.

**New types:**
- `contract/TrainingObservation.kt` — `TrainingObservation(globals, players[], zones, stack, pendingDecision?, perspectivePlayerId, stateDigest)`
- `contract/EntityFeatures.kt` — flat per-card features (id, controller, owner, zone, tapped, summoning-sick, pt, types-bitset, keywords-bitset, counters)
- `contract/LegalActionView.kt` — `(actionId: Int, kind: String, sourceEntityId?, targetEntityIds[], manaCost?, affordable: Boolean)`
- `contract/ObservationBuilder.kt` — builds `TrainingObservation` from `GameState` + `perspectivePlayerId` + `agentToAct`, applies opponent-hand masking
- `contract/ActionRegistry.kt` — per-step `Int -> LegalAction` mapping, regenerated every `step()` (documented as contract guarantee — not cacheable across steps)

**Key decisions baked in:**
- Observation is structured-nested JSON (zones = arrays of entity objects) with fixed schema / stable key order — Python side can vectorize without a discovery pass.
- `stateDigest: String` included in every observation for future MCTS transposition tables.
- Opponent hand rendered as `{zone: "HAND", count: 5, cards: []}` in the perspective view.

**Acceptance signal:** Kotlin test that resets a sealed game, dumps `TrainingObservation` via Jackson, looks up `actionId=k`, calls `step()`, and snapshot-asserts the new JSON shape.

---

## Phase 2 — In-JVM env registry + batched API surface (`gym-server`)

**Deliverable:** New `gym-server/` Gradle module with transport-agnostic service.

**New files:**
- `gym-server/build.gradle.kts` — Spring Boot Web only. Depends on `rules-engine`. No WebSocket, no JPA.
- `service/MultiEnvService.kt` — `ConcurrentHashMap<EnvId, GameEnvironment>`. Methods: `create(config)`, `reset(ids)`, `stepBatch(List<StepRequest>)`, `legalActionsBatch`, `fork(srcId)`, `dispose(ids)`, `snapshot(id)`, `restore(id, snapshot)`.
- `service/EnvWorkerPool.kt` — `ForkJoinPool` sized to `Runtime.availableProcessors()`. Each env is single-threaded; envs run in parallel.
- `service/SnapshotCodec.kt` — in-process (free, immutable state reference) or cross-process (serialized bytes). Opaque handle hides the distinction.
- `service/DeckResolver.kt` — resolves a deck spec (sealed code OR explicit card list) to a `Map<String, Int>`. Delegates sealed to existing `SealedDeckGenerator`/`HeuristicDeckBuilder`.

**Tradeoff:** cross-process snapshots require `GameState` serialization — defer actual implementation until needed; in-process slot IDs cover Phase 5 MCTS.

**Acceptance signal:** Microbenchmark in `benchmarks/` driving 64 parallel random-vs-random rollouts through `MultiEnvService.stepBatch`. Target: within 2× of direct `GameEnvironment.playGame` loop.

---

## Phase 3 — HTTP transport (`gym-server`)

**Deliverable:** REST controllers exposing `MultiEnvService` over HTTP+JSON.

**Endpoints:**
- `POST /v1/envs` — create N envs. Body: `{count, gameConfig: {player1Deck, player2Deck, skipMulligans?, revealAll?}}`. Deck = `{sealedSetCode}` OR `{cards: [{name, count}]}`. Returns `[envId]`.
- `POST /v1/envs/reset` — body `{envIds}` → `[{envId, observation, legalActions, terminated}]`
- `POST /v1/envs/step` — body `[{envId, actionId}]` (**batched**) → `[{envId, observation, legalActions, reward, terminated, truncated, info}]`
- `POST /v1/envs/fork` — body `{srcEnvId, count}` → `[envId]`
- `DELETE /v1/envs` — body `{envIds}`
- `GET /v1/envs/{id}/snapshot` / `POST /v1/envs/{id}/restore`
- `POST /v1/decks/validate` — body `{cards: [{name, count}], format?: "SEALED"|"CONSTRUCTED"}` → `{ok, errors[]}`. Checks unknown cards, min-size, banned-list. For MageZero-style deck submission.
- `GET /v1/sets` — list registered sets (all of them, per plan) with metadata
- `GET /v1/health`
- `GET /v1/spec` — `{schemaHash, serverVersion, schema}` for client compatibility checking

**Why HTTP first:**
- Lowest-friction install for RL researchers (no protoc, no native deps).
- Batching collapses per-call overhead — N envs = one TCP round-trip per training tick.
- gRPC and shared-memory remain viable Phase 6 optimizations once profiling identifies a bottleneck.

**Acceptance signal:** `curl` script creates 8 envs, 100 random steps each, dumps timings. Integration test in `gym-server/src/test/`.

---

## Phase 4 — Python `argentum-gym` package (colocated)

**Deliverable:** `python/argentum_gym/` directory, pip-installable, publishable later.

**Layout:**
- `python/pyproject.toml` — package `argentum-gym`. Deps: `gymnasium`, `numpy`, `httpx`, `pydantic`.
- `python/argentum_gym/client.py` — `ArgentumClient` thin wrapper over `httpx.Client` with a connection pool. Methods: `step_batch`, `reset_batch`, `create`, `fork`, `snapshot`, `validate_deck`.
- `python/argentum_gym/env.py` — `ArgentumEnv(gymnasium.Env)`. `observation_space` = `Dict` space; `action_space` = `Discrete(MAX_ACTIONS=1024)`; `info["action_mask"]` follows the MaskablePPO convention (sb3-contrib).
- `python/argentum_gym/vector_env.py` — `ArgentumVectorEnv` implementing `gymnasium.vector.VectorEnv` directly. Batching lives server-side; this is the canonical training entry point.
- `python/argentum_gym/encoder.py` — JSON → numpy. Per-entity float matrix + per-zone index masks. Schema-driven from `/v1/spec`.
- `python/argentum_gym/launcher.py` — optional JVM subprocess helper for notebooks/CI. Default mode: user runs the server separately.
- `python/argentum_gym/deck.py` — helpers for deck construction / validation (`Deck.from_file`, `Deck.sealed("BLB")`, `client.validate(deck)`).

**Design notes:**
- `MAX_ACTIONS=1024` fixed cap, masked padding. Variable-size action spaces are a constant pain in RL libs; mask + fixed cap is the standard escape hatch.
- `pendingDecision` exposed as a typed feature in the observation so the policy knows *why* it's choosing (targeting vs. scry vs. choose-X), even though the action ID space is unified.

**Acceptance signal:** `pytest` running 4 parallel envs against a real `gym-server` for 200 steps. Asserts reward shape, masked-illegal actions never execute, and constructed-deck submission round-trips.

---

## Phase 5 — MCTS / AlphaZero primitives (next step)

**Deliverable:** Server-side primitives for efficient simulation loops.

**New endpoints:**
- `POST /v1/envs/{id}/rollout` — `{policy: "random"|"heuristic", count: N, maxSteps}` → `[{rewards, finalSnapshot?}]`. Server-side parallel rollouts via `EnvWorkerPool`. Neural-net policy rollouts can come later; random/heuristic covers bootstrap and leaf evaluation for lightweight MCTS.
- `POST /v1/envs/expand` — `{envId, actionIds[]}` → `[{childEnvId, observation, legalActions}]`. One round-trip per node expansion instead of N.

**Extras:**
- `stateDigest` already in every observation (Phase 1) → Python-side transposition tables.
- `snapshot()` returns in-process slot IDs by default (free, since `GameState` is immutable and shared). `?format=blob` returns serialized bytes for cross-process tree sharing. Blob path deferred.

**Tradeoff (state diffs vs. full snapshots):** rejected lightweight diffs. Immutable `GameState` already makes in-process "diffs" free via shared structure. Cross-process diff encoder = high maintenance cost, dubious win; recompute-on-demand is fine.

**Acceptance signal:** Python MCTS reference in `python/argentum_gym/contrib/mcts.py` — 100 sims per move, beats random >70% over 50 games.

---

## Phase 6 — Profiling & transport optimizations (deferred)

Only after Phase 5 ships with real measurements:
- Replace JSON observation with msgpack/FlatBuffers if encode cost dominates.
- Add gRPC variant of `MultiEnvService` if HTTP framing is the bottleneck.
- Co-locate trainer + JVM via Py4J/JNI only if cross-process IPC is shown to dominate.

Do not pre-optimize. HTTP+batched path will saturate one machine long before transport matters.

---

## Don't do (scope creep to refuse)

- ❌ Training code, model architectures, or pretrained weights. Package's job is fast, correct environment.
- ❌ Generic "MTG card → feature vector" embedding service. Ship the raw structured observation; trainers own their encoders.
- ❌ Shared serializer between `ClientStateTransformer` and the training observation. Different audiences, different schemas.
- ❌ WebSockets in `gym-server`. RL training is request/response; streaming adds backpressure complexity for zero gain.
- ❌ JNI/Py4J in v1.
- ❌ Multi-JVM env clusters.
- ❌ Exposing engine internals (`ContinuationFrame`, `ProjectedState` internals) in the contract — only their observable effects.

---

## Critical files referenced

- `rules-engine/src/main/kotlin/com/wingedsheep/engine/gym/GameEnvironment.kt`
- `rules-engine/src/main/kotlin/com/wingedsheep/engine/gym/StepResult.kt`
- `rules-engine/src/main/kotlin/com/wingedsheep/engine/gym/ActionSelector.kt`
- `rules-engine/src/main/kotlin/com/wingedsheep/engine/ai/HeuristicDeckBuilder.kt`
- `game-server/src/main/kotlin/com/wingedsheep/gameserver/deck/SealedDeckGenerator.kt`
- `game-server/src/main/kotlin/com/wingedsheep/gameserver/legalactions/LegalActionEnricher.kt` (reference only; not reused — training view is separate)
- `settings.gradle.kts` (add `gym-server` module)