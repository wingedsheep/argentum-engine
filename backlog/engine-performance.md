# Engine Performance — MCTS / AI Training Optimization

Benchmark results (Portal sealed, random actions, 100 games on 8 threads):
- **~2,000 actions/sec per thread** (JIT-warmed)
- **~10 games/sec wall clock** (8 cores)
- **Time split: 48% legal action enumeration, 52% action processing**

For comparison, MageZero (XMage-based) achieves ~150 MCTS sims/sec single-thread, bottlenecked by
mutable state deep-cloning. Argentum's immutable state gives us free forking but we're spending time
on redundant recomputation instead.

---

## Phase 1 — Quick Wins (no API changes)

### 1. ~~Cache mana sources per enumeration pass~~ ✅ Done

**Problem:** `ManaSolver.findAvailableManaSources()` scans the entire projected battlefield on every
call — checking tapped status, summoning sickness, haste, subtypes, and abilities for each permanent.
It's called from `canPay()` (which internally calls `solve()` 1-2× each), `solve()` (for auto-tap
preview), and `getAvailableManaCount()`.

With 5 cards in hand and alternative/kicker costs, a single `enumerate()` pass calls
`findAvailableManaSources()` **20-25 times per castable card** — all returning identical results since
the board hasn't changed.

**Fix:** Compute `findAvailableManaSources()` once in `EnumerationContext` (which already provides
lazy-cached `projectedState` and `battlefieldPermanents`) and pass it into the solver. The mana source
list is immutable within a single enumeration pass.

**Files:**
- `EnumerationContext.kt` — add `val availableManaSources` lazy property
- `ManaSolver.kt` — add optional `precomputedSources` parameter to `canPay()`, `solve()`,
  `getAvailableManaCount()`
- `CastSpellEnumerator.kt`, `ActivatedAbilityEnumerator.kt`, `CastFromZoneEnumerator.kt` — pass
  cached sources

**Impact:** ~30-40% reduction in enumeration time. Highest-value single change.

### 2. ~~Skip auto-tap preview in MCTS mode~~ ✅ Done

**Problem:** `CastSpellEnumerator` calls `ManaSolver.solve()` separately after `canPay()` to compute
`autoTapPreview` (which lands to tap for the client UI). For MCTS, only the `GameAction` matters — not
the tap preview.

**Fix:** Add a `skipAutoTapPreview` flag to `EnumerationContext` (or a separate `EnumerationMode` enum).
When set, `CastSpellEnumerator` skips the `solve()` call for preview and leaves `autoTapPreview = null`.

**Files:**
- `EnumerationContext.kt` — add `mode: EnumerationMode` field (`FULL` / `ACTIONS_ONLY`)
- `CastSpellEnumerator.kt` — skip preview `solve()` calls (~lines 256, 270, 278, 376)
- `LegalActionEnumerator.kt` — accept mode parameter
- All other enumerators with auto-tap solve calls: `CastFromZoneEnumerator`, `ActivatedAbilityEnumerator`,
  `CyclingEnumerator`, `MorphCastEnumerator`, `TurnFaceUpEnumerator`, `GraveyardAbilityEnumerator`

**Impact:** ~25% reduction in enumeration time for MCTS use case.

### 3. Cache trigger index within PassPriority

**Problem:** `TriggerDetector.buildTriggerIndex()` scans the entire battlefield twice per build (once
for grant providers, once for categorization). `PassPriorityHandler` calls `detectTriggers()` and
`detectPhaseStepTriggers()` separately, each rebuilding the index from scratch. During stack
resolution, trigger detection runs 2-3 times.

**Fix:** Build the trigger index once per PassPriority execution and reuse it across
`detectTriggers()` and `detectPhaseStepTriggers()` calls. Add an optional `prebuiltIndex` parameter
to both methods.

**Files:**
- `TriggerDetector.kt` — add `prebuiltIndex` parameter to `detectTriggers()` and
  `detectPhaseStepTriggers()`; extract `buildTriggerIndex()` as a public method
- `PassPriorityHandler.kt` — build once, pass to all detect calls within the same execution

**Impact:** ~15-20% reduction in processing time. Eliminates 2-3 redundant battlefield scans per
PassPriority.

### 4. ~~Skip undo policy computation~~ ✅ Done

**Problem:** `UndoPolicyComputer.compute()` runs on every action, classifying the action and scanning
all events for information-revealing properties. Then `result.copy(undoPolicy = ...)` allocates a new
`ExecutionResult`. Undo is a UI feature irrelevant to simulation.

**Fix:** Add a constructor flag to `ActionProcessor` (e.g., `computeUndo: Boolean = true`). When
false, skip the computation and copy.

**Files:**
- `ActionProcessor.kt` — add flag, skip lines 76-77 when disabled

**Impact:** Small per-action savings but zero-risk. Eliminates one `ExecutionResult.copy()` and one
event scan per action.

---

## Phase 2 — Structural Improvements

### 5. Slim enumeration mode for MCTS

**Problem:** `LegalAction` is a large data class (30+ fields) carrying targeting metadata, convoke
creature info, delve cards, crew data, damage distribution info, etc. MCTS only needs the `GameAction`
and whether it's affordable. Computing and allocating the full `LegalAction` per option is wasteful.

**Fix:** Create a `SlimLegalAction(action: GameAction, affordable: Boolean)` return type. Add a
separate `enumerateActions()` method to `LegalActionEnumerator` that skips enrichment. Internally, each
sub-enumerator gets an early-out path that returns just the action + affordability.

**Files:**
- `LegalAction.kt` — add `SlimLegalAction` data class
- `LegalActionEnumerator.kt` — add `enumerateActions()` method
- All enumerators — add slim path (skip target enumeration, convoke/delve metadata, etc.)

**Impact:** Reduces allocation pressure and computation per action. Most fields in `LegalAction`
require additional queries (valid targets, convoke creatures, etc.) that are pure waste for MCTS.

### 6. Lazy event collection

**Problem:** Every handler allocates a `mutableListOf<GameEvent>()` and accumulates events even when
no client is listening. Events are appended to `ExecutionResult` via list concatenation
(`events + newEvents`), creating intermediate lists.

**Fix:** For MCTS mode, use an event sink that counts events but doesn't store them (triggers still
need event types for detection, but not the full list). Alternatively, use a reusable
`ArrayList<GameEvent>` with pre-allocated capacity instead of creating new lists.

**Files:**
- `ExecutionResult.kt` — consider an `EventSink` interface with `Full` and `CountOnly` implementations
- All handlers that create event lists

**Impact:** Reduces GC pressure significantly. A typical game produces thousands of events.

### 7. Reduce GameState copies per zone transition

**Problem:** Moving a card between zones currently produces up to 4 `GameState.copy()` calls:
`removeFromZone()` → `addToZone()` → `updateEntity()` (strip components) → `updateEntity()`
(add entry-turn component). Each `copy()` creates a new data class instance with 19 fields.

**Fix:** Batch zone transition mutations into a single `GameState.copy()` call. Create a
`GameState.moveEntity(entityId, fromZone, toZone, componentUpdates)` method that applies all changes
in one copy.

**Files:**
- `GameState.kt` — add batched zone transition method
- Zone-change handlers that call `removeFromZone` + `addToZone` separately

**Impact:** 2-4× fewer state allocations per zone change. Zone changes are frequent (every creature
death, every spell resolution).

---

## Phase 3 — Advanced / Experimental

### 8. Incremental state projection

**Problem:** `StateProjector.project()` rebuilds projected state from scratch for every `GameState`
instance (even when the board hasn't materially changed). Each projection iterates the entire
battlefield, collects all continuous effects, and applies layers. Accessed via `lazy` on `GameState`,
but `copy()` resets the lazy.

**Fix:** Track a "projection-relevant hash" (battlefield entity set + continuous effects + counters).
If the hash matches a previous projection, reuse it. Alternatively, make projection incremental: track
which entities changed and only re-project those.

**Complexity:** High. Layer dependencies (Rule 613.8) mean changing one effect can cascade across
the board. Incremental projection needs careful invalidation.

**Impact:** Would reduce processing time significantly for actions that don't change the board (most
PassPriority actions during priority passing).

### 9. Persistent data structures for GameState

**Problem:** Kotlin's default `Map` and `List` are backed by Java's `HashMap`/`ArrayList`. The `+`
operator on maps creates a full shallow copy. With 19 fields and multiple maps, each `GameState.copy()`
does substantial allocation work.

**Fix:** Use persistent/functional data structures (e.g., `kotlinx.collections.immutable`
`PersistentMap`, `PersistentList`) that share structure between versions. Adding/removing an element
creates a new "version" in O(log n) with shared tree nodes instead of copying the entire map.

**Complexity:** Medium. Requires changing `GameState` field types and updating all map/list operations.
Performance characteristics change (O(log n) lookup instead of O(1) but much cheaper "copy").

**Impact:** Could significantly reduce allocation pressure for state transitions, especially for MCTS
where millions of states are forked and discarded.

### 10. Dedicated MCTS `ActionProcessor` configuration

**Problem:** The current `ActionProcessor` is optimized for correctness and server use — it always
computes undo policy, collects events, builds full legal actions, etc. For MCTS, many of these are
unnecessary.

**Fix:** Create an `EngineServices` configuration mode (or a separate `SimulationProcessor`) that
disables:
- Undo policy computation (#4)
- Auto-tap preview in enumeration (#2)
- Full event list accumulation (#6)
- Legal action enrichment (#5)

This composes the Phase 1/2 optimizations into a single "simulation mode" entry point.

**Files:**
- `EngineServices.kt` — add `SimulationMode` configuration
- `ActionProcessor.kt` — respect mode
- `LegalActionEnumerator.kt` — respect mode

**Impact:** Combines all above optimizations. Target: **5,000-10,000 actions/sec per thread**
(~3-5× current throughput).

---

## Implementation Plan

```
Phase 1 (quick wins, no API breaks):
  1. Cache mana sources in EnumerationContext        → ~30-40% enumeration speedup
  2. Skip auto-tap preview flag                      → ~25% enumeration speedup
  3. Cache trigger index in PassPriority             → ~15-20% processing speedup
  4. Skip undo policy flag                           → small processing speedup

Phase 2 (structural, minor API additions):
  5. Slim enumeration mode                           → less allocation per action
  6. Lazy event collection                           → less GC pressure
  7. Batched zone transitions                        → 2-4× fewer state copies per zone change

Phase 3 (advanced, experimental):
  8. Incremental state projection                    → fewer redundant projections
  9. Persistent data structures                      → structural sharing for state forks
  10. Dedicated simulation mode                      → composes all optimizations
```

Target: Phase 1 alone should bring us from ~2,000 to ~4,000 actions/sec per thread. Phase 2 adds
another ~50%. Phase 3 is speculative but could push toward 10,000+.
