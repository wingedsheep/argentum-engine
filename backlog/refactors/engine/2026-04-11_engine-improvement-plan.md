# Rules-Engine Improvement Plan — 2026-04-11

## Context

This document audits the current state of `rules-engine/` against
[`docs/architecture-principles.md`](../../../docs/architecture-principles.md) and identifies
concrete improvements, grouped by tier of importance. It is a **successor** to
[`2026-02-10_architecture-review.md`](./2026-02-10_architecture-review.md) — several issues from
that review have been resolved, and this document tracks remaining drift, new concerns, and
elegance / extensibility wins.

Scope: 700 Kotlin files under `rules-engine/src/`, 201 effect executors, 22 continuation resumer
modules.

### Progress since 2026-02-10

The following findings from the previous review are resolved and are **not** tracked here:

| Old finding | Status |
|---|---|
| StateProjector O(n²) without caching | ✅ Fixed — `GameState.projectedState` is `by lazy`, cached per immutable snapshot (`state/GameState.kt:109`) |
| ContinuationHandler 1700-line god class | ✅ Fixed — now 78 lines; dispatch via `ContinuationResumerRegistry` across 22 modules |
| `StackResolver()` instantiated ad hoc in executors | ✅ Fixed — no fresh instances remain |
| `ExecutionResult` vs `EngineResult` dual types | ✅ Fixed — single `ExecutionResult` everywhere |
| `ActionProcessor` monolithic dispatch | ✅ Clean registry, no giant `when` |
| Wall-clock timestamps in floating effects | ✅ Fixed in `5208025b3` — `FloatingEffectFactory` defaults to `state.timestamp`; a `NoWallClockInGameLogicTest` hygiene test guards against regression |
| `StateBasedActionChecker` unbounded loop | ✅ Fixed in `a2ebf4827` — capped at 1000 iterations, ends game as draw per rule 104.4c |
| `ConditionEvaluator` / `PredicateEvaluator` TODOs (`EnteredThisTurn`, damage tracking) | ✅ Fixed in `92907fbd0` — 7 TODOs resolved via new ECS tracking components |

The architecture's **bones are good**. What remains is a mix of (a) determinism bugs that threaten
replay/networking, (b) drift from stated principles, and (c) an accumulation of bespoke
executors and duplication that is making each new card more expensive than it should be.

---

## Tier 1 — Critical (correctness / principle drift)

These block replayability or represent direct violations of stated principles. Fix first.

### 1.1 Enforce "controlled by" queries via projected state everywhere ✅

**Status.** Fixed on 2026-04-11. `GameState.controlledBattlefield(playerId)` is the canonical
helper for "what does this player control on the battlefield"; it forwards to
`projectedState.getBattlefieldControlledBy(...)`. The docstring on
`GameState.getBattlefield(playerId)` now spells out that it returns the **owner-keyed** zone, not
the controlled-by view, and points readers at the new helper.

The three remaining base-state battlefield-by-controller reads inside `handlers/effects/` were
fixed in the same change:

| Site | Fix |
|---|---|
| `DamageUtils.kt:751` (NoncombatDamageBonus scan) | now iterates `state.controlledBattlefield(sourceController)` and resolves `sourceController` via `projected.getController(sourceId)` first |
| `LookAtFaceDownExecutor.kt:73` (look at face-down creatures a player controls) | switched to `state.controlledBattlefield(targetPlayerId)` so Annex'd / Threaten'd face-down creatures are revealed correctly |
| `WarpExileExecutor.kt:44` (presence check on warp exile delayed trigger) | simplified to the no-arg `state.getBattlefield()`, which is the right primitive for an "is the entity still on the battlefield somewhere" check |

The token-creation executors flagged in the original write-up (`CreateTokenExecutor.kt:146`,
`CreateTokenCopyOfSourceExecutor.kt:94`, etc.) turned out to be **writes**, not reads, and the
`controllerId` they pass to `ZoneKey(_, BATTLEFIELD)` is already the projected controller of the
source ability — `TriggerDetector` resolves that via `projected.getController(entityId)` at
line 104. So there is no token-zone bug to fix; the write goes to the correct owner-keyed zone
because tokens are owned by the player who created them.

The "make `ZoneKey(_, BATTLEFIELD)` access `internal`" idea from the original fix list was
**not** taken: writes (token creation, ETB resolution) legitimately need to address a specific
owner-keyed zone, and clamping the visibility would force all those write paths through
indirection without any new safety. The naming + docstring split between `getBattlefield(playerId)`
and `controlledBattlefield(playerId)` is sufficient guard.

---

### 1.2 Complete the placeholder cost / decision handlers — ✅ Resolved

**Why it mattered.** Principle §2.10 requires costs to be validated and executed uniformly.
Three pieces of the payment / decision pipeline were stubs; cards that touched them would have
silently misbehaved.

**Resolution.**
- `CostHandler.AbilityCost.DiscardSelf` now moves the source card from its owner's hand to the
  graveyard, emits `CardsDiscardedEvent` + `ZoneChangeEvent`, and fails cleanly when the card
  isn't in hand. Covered by `CostHandlerDiscardSelfTest`.
- `MakeChoiceHandler` / `SelectTargetsHandler` — **deleted** along with the `MakeChoice` and
  `SelectTargets` `GameAction` types (plus Serialization registration and the web-client type
  mirrors). These actions were fully orphaned: nothing in the engine, sets, tests, or game-server
  ever constructed them. Modal spell mode selection and target selection already go through
  `SubmitDecision` + `ChooseModeDecision`/`ModesChosenResponse` and
  `ChooseTargetsDecision`/`TargetsResponse`, dispatched by `SubmitDecisionHandler`. Eliminating
  the stub path is the uniform answer per §2.10; the real path is already exercised by the
  modal-spell scenario tests (`castSpellWithMode` across `LongRiversPullScenarioTest`,
  `JollyGerbilsScenarioTest`, `CrumbAndGetItScenarioTest`, `DawnsTruceScenarioTest`) and every
  targeted-spell scenario test in the corpus.

---

## Tier 2 — High (principle drift, maintenance tax)

These aren't bugs, but they make each new card more expensive than it should be and they're
visible violations of stated principles.

### 2.1 Decompose monolithic executors into atomic pipelines ✅ (tracked)

**Why it matters.** Principle §1.5 is explicit: "Adding a card that says 'Look at the top 5, put
2 on bottom, rest on top' requires zero new effect executors." Several executors in the current
tree ignore this and implement whole mechanics monolithically, which means future similar cards
will spawn sibling monoliths rather than reusing the pipeline.

Items already tracked on
[`atomic-effect-decomposition.md`](../atomic-effect-decomposition.md) and the
[`sdk-composability/`](../sdk-composability/) folder cover most of the known SDK gaps and should
be progressed. The following findings were added on 2026-04-11:

| Executor | Lines | Why it's wrong | Desired shape | Tracked at |
|---|---|---|---|---|
| `handlers/effects/library/RevealUntilExecutor.kt` | 136 | Gather + filter + reveal baked together | `Gather(top N) → Filter → Reveal → Move` via atomic primitives | [`atomic-effect-decomposition.md` → `RevealUntilEffect`](../atomic-effect-decomposition.md) |
| `handlers/effects/removal/ExileUntilLeavesExecutor.kt` | 77 | Exile + linked-return bundled | `MoveToZone(exile, storeAs) + StoredEntityTarget` return trigger | [`atomic-effect-decomposition.md` → `ExileUntilLeavesEffect`](../atomic-effect-decomposition.md) |
| `handlers/effects/library/PutCreatureFromHandSharingTypeExecutor.kt` | 98 | Context-aware filter baked into executor | Replace with `GatherCards(hand, filter=SharesSubtypeWith(ctxKey))` once filter supports stored references | [`atomic-effect-decomposition.md` → `PutCreatureFromHandSharingTypeWithTappedEffect`](../atomic-effect-decomposition.md) |
| `handlers/effects/drawing/DrawCardsExecutor.kt` | 429 | God executor: draw + replacement shields + prompt-on-draw + draw-step reveal | Split into `DrawCardPrimitiveExecutor` + `DrawReplacementDispatcher` | [`decompose-draw-cards-executor.md`](./decompose-draw-cards-executor.md) |

**Status.** Each row now has a tracked item. The first three share SDK-level prerequisites
(`storeAs` on `MoveToZoneEffect`, `StoredEntityTarget`, `SharesSubtypeWith(contextKey)` filter,
`CardSource.LibraryStreaming` + standalone `RevealCollectionEffect`); see the "New Primitives
Needed" table at the bottom of `atomic-effect-decomposition.md`. The `DrawCardsExecutor` ticket is
a pure engine refactor with no SDK prerequisites and can proceed independently.

---

### 2.2 Extract `EffectContext.resolveTarget()` helper ✅

**Status.** Fixed on 2026-04-11. `EffectContext` now exposes member methods that delegate to
`TargetResolutionUtils`:

```kotlin
fun resolveTarget(target: EffectTarget): EntityId?
fun resolveTarget(target: EffectTarget, state: GameState): EntityId?
fun requireTarget(target: EffectTarget): EntityId                    // throws on null
fun requireTarget(target: EffectTarget, state: GameState): EntityId  // throws on null
fun resolvePlayerTarget(target: EffectTarget): EntityId?
fun resolvePlayerTarget(target: EffectTarget, state: GameState): EntityId?
fun resolvePlayerTargets(target: EffectTarget, state: GameState): List<EntityId>
```

All 87 executor files migrated from `TargetResolutionUtils.resolveTarget(effect.target, context, state)`
to `context.resolveTarget(effect.target, state)` (and the player-target equivalents). Member imports
of `TargetResolutionUtils.resolveTarget` / `.resolvePlayerTarget` / `.resolvePlayerTargets` were
cleaned up across the tree. The utility object remains as the backing implementation and still hosts
`toEntityId` (an extension on `ChosenTarget`), which intentionally stays put.

---

### 2.3 Reorganize `handlers/effects/` subfolders around intent, not legacy groupings

**Why it matters.** 201 files across 16 folders with no clear principle. Two folders in particular
are muddled:

- **`handlers/effects/removal/`** (23 files) — a holdover from the pre-`EffectTarget` refactor.
  Today it mixes zone-change executors (`MoveToZoneEffectExecutor`, `ExileUntilLeavesExecutor`)
  with things that aren't removal at all (`PutOnTopOrBottomOfLibraryExecutor`).
- **`handlers/effects/permanent/`** (44 files) — mixes stat modification (`AddCountersExecutor`),
  control changes (`ExchangeControlExecutor`), type changes (`BecomeCreatureExecutor`), and
  link-tracking (`GrantExileOnLeaveExecutor`).

**Fix.** One of:
1. Rename `removal/` → `zone-transitions/`, move library-specific executors into `library/`, move
   exile-link logic into a dedicated `linked-exile/` subfolder.
2. Split `permanent/` into `permanent/counters/`, `permanent/control/`, `permanent/types/`,
   `permanent/attachments/`.

Either is fine; the point is that a new contributor should be able to guess the folder for a new
executor without reading the existing tree. This is pure file movement, no logic changes, and
pairs well with 2.1 (decomposing the monoliths means fewer of these executors exist in the first
place).

---

### 2.4 Audit `Serialization.kt` polymorphic registration with a compile-time check

**Why it matters.** The previous review listed ~20 missing polymorphic registrations across
`Component`, `ContinuationFrame`, `PendingDecision`, `DecisionResponse`, and `GameEvent`. A spot
check today finds no obvious gaps, but the manual list will drift again the next time someone adds
a component or event. Principle §2.4 depends on continuations being fully serializable.

**Fix.** Add a test that walks every sealed class under those hierarchies via reflection and
asserts each concrete subclass is registered in the `SerializersModule`. Cheap to write (50 lines),
bulletproof going forward. Put it in `rules-engine/src/test/` so it runs in `just test-rules`.

---

### 2.5 Consolidate counter-placement executors

**Why it matters.** Three nearly-identical executors:
- `handlers/effects/permanent/AddCountersExecutor.kt` (63 lines, single target)
- `handlers/effects/permanent/AddCountersToCollectionExecutor.kt` (collection target)
- `handlers/effects/permanent/AddDynamicCountersExecutor.kt` (single target, dynamic amount)

They share: counter-type parsing (~10 lines each, `.replace('+', 'P').replace('-', 'M')` fallback),
replacement-effect application, event emission. `DistributeCountersAmongTargetsExecutor` and
`DistributeCountersFromSelfExecutor` have the same parsing code again.

**Fix.** Extract `CounterTypeParser.parse(String): CounterType` (pure, ~20 lines). Either unify
the three `Add*` executors into one that accepts `EffectTarget.Collection | EffectTarget.Single`,
or share a `CounterPlacementHelper` that all four executors call. Replacement-effect modifiers
stay in `ReplacementEffectUtils.applyCounterPlacementModifiers` where they already live.

---

## Tier 3 — Medium (polish, maintainability)

Not urgent, but each one reduces cognitive load for future work.

### 3.1 Split `state/components/battlefield/BattlefieldComponents.kt` by concern

365 lines, ~40 components spanning saga progression, linked exile, attachments, counter placement,
ability-firing state, and one-shot turn markers. Components themselves are pure data (ECS hygiene
is fine), but the file is becoming a pain to navigate. Split into:

- `SagaComponents.kt` — `SagaComponent`, `ClassLevelComponent`
- `LinkedExileComponents.kt` — `LinkedExileComponent`, `AttachedToComponent`, `AttachmentsComponent`
- `AbilityTrackingComponents.kt` — `AbilityActivatedThisTurnComponent`, `TriggeredAbilityFiredThisTurnComponent`
- `BattlefieldComponents.kt` — keep the core markers (`TappedComponent`, `SummoningSicknessComponent`, `DamageComponent`, `CountersComponent`)

Pure file movement, zero logic change. Do after 2.3 so imports get touched once.

### 3.2 Remove or use `AttachmentsComponent`

Flagged in the previous review and still true: `AttachmentsComponent` appears to be a dead
data definition — nothing reads it, nothing maintains it. Either wire it up (so every aura /
equipment attachment updates both sides of the relationship) or delete it. Preference: delete
it and let the single `AttachedToComponent` on the attached entity be the source of truth,
since nothing currently depends on reverse lookups.

Location: `state/components/battlefield/BattlefieldComponents.kt:137`.

### 3.3 Audit 47 `!!` assertions under `handlers/effects/`

Most are legitimate ("pending decision must exist here by construction"), but each one is a future
NPE waiting for a corner case. A 30-minute pass can replace the legitimate ones with
`requireNotNull(...) { "..." }` (self-documenting) and the defensive ones with proper early returns.

### 3.4 Document the "entities persist in the entity map after zone changes" invariant

Several subsystems — `TriggerDetector.detectLeavesBattlefieldTriggers`, graveyard-dies triggers,
sacrifice-as-cost triggers — rely on the fact that entities remain in `state.entities` even after
they leave a zone, so "look back in time" lookups work (principle §2.5, Rule 603.10). This is
load-bearing but undocumented. Add a note to `state/GameState.kt` explaining the invariant, and
a test that would fail loudly if someone ever added aggressive entity-map GC.

---

## Tier 4 — Low (nice to have)

### 4.1 Observability hooks

The engine is completely silent. Adding optional structured logging at action boundaries
(`ActionProcessor.process`) and continuation resume points (`ContinuationHandler.resume`) would
radically improve debuggability without coupling the engine to any specific logging framework —
inject a `EngineTelemetry` interface with a no-op default and a production impl in `game-server`.

### 4.2 Hand smoother `takeLast` → `take`

Cosmetic: `HandSmoother` uses `library.shuffled().takeLast(count)` where `take` would be more
idiomatic. Still random, but the current form is mildly confusing. Zero gameplay impact.

### 4.3 Legend rule player choice

Rule 704.5j says *the player* chooses which legendary permanent to keep. The current
implementation always keeps the first one. Low priority because it only matters in corner cases,
but it's a rules-correctness issue.

### 4.4 Mana solver: two-pass constraint satisfaction

The current greedy mana solver handles 95% of cases well; the remaining 5% (dual lands in the
presence of future commitments) could use a two-pass "constrained colors first, flexible generic
last" approach. Not urgent — today's solver is already better than Arena in most situations.

### 4.5 Naming cleanup

- Folder rename `handlers/effects/removal/` (see 2.3)
- Several executors have names that describe their only current use rather than their mechanic
  (e.g., `PutCreatureFromHandSharingTypeExecutor`). After decomposition (2.1), these names will go
  away naturally; no separate ticket needed.

---

## Cross-references to existing backlog

This document deliberately does **not** re-list items already tracked elsewhere. See:

- [`backlog/refactors/atomic-effect-decomposition.md`](../atomic-effect-decomposition.md) — SDK-side
  atomic effects, Oblivion Ring pattern, `StoreEntityRefEffect`, linked-exile decomposition
- [`backlog/refactors/legal-actions-calculator.md`](../legal-actions-calculator.md) — legal-action
  enumeration refactors
- [`backlog/refactors/sdk-composability/`](../sdk-composability/) — 13 SDK composability items
  (most Tier 1 + Tier 2 items done; Tier 3 partially done)
- [`backlog/refactors/sdk-refactor/`](../sdk-refactor/), [`sdk-architecture-improvements.md`](../sdk-architecture-improvements.md)
  — SDK-side architectural work

---

## Summary table

| Tier | # | Theme | Count |
|---|---|---|---|
| 🔴 1 Critical | 1.1–1.2 | Zone-model drift, stub handlers | 2 |
| 🟠 2 High | 2.1–2.5 | Monolithic executors, duplication, folder drift, serialization hygiene | 5 |
| 🟡 3 Medium | 3.1–3.4 | File organization, dead code, invariants | 4 |
| 🟢 4 Low | 4.1–4.5 | Observability, naming, minor rules corrections | 5 |

**Recommended order of attack:**
1. ~~Tier 1.1 — add the `controlledBattlefield` helper, then grep-and-replace every raw `ZoneKey(_, BATTLEFIELD)` inside `handlers/effects/`.~~ ✅ Done.
2. ~~Tier 2.1 — audit monolithic executors and file decomposition tickets.~~ ✅ Tracked (see 2.1 above). Implementation of each ticket proceeds when the set it serves comes up.
3. ~~Tier 2.2 (`EffectContext.resolveTarget`) — single mechanical pass, unlocks downstream cleanup of every effect executor.~~ ✅ Done.
4. Tier 2.3 (folder reorg) once 2.1 tickets have stabilized so file moves don't churn in-flight work.
5. Tier 2.4 (serialization reflection test) — cheap, high insurance value.
6. Tier 1.2 and everything else opportunistically.
