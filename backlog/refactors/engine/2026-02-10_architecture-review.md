# Architecture Review: MTG Game Engine

## Executive Summary

This is a well-structured, immutable-state MTG game engine built on ECS principles with a continuation-passing style for
async player decisions. The codebase demonstrates strong architectural fundamentals — pure functional state transitions,
serializable game state, and clean separation of concerns. However, several areas warrant attention ranging from
critical correctness issues to longer-term maintainability concerns.

---

## 🔴 Critical (Fix Now)

<details>
<summary><strong>1. StateProjector is called repeatedly without caching, causing O(n²) performance in combat</strong></summary>

`StateProjector.project()` is called dozens of times per action — in `CombatManager`, `TurnManager`, `TargetValidator`,
`TargetFinder`, `PredicateEvaluator.matchesWithProjection`, and many effect executors. Each call iterates all
battlefield entities, collects floating effects, and sorts by layer. In a board state with 20+ creatures and multiple
floating effects, this becomes a serious bottleneck.

**Worst case**: `CombatManager.applyCombatDamage()` calls `stateProjector.project(state)` once at the top, but then
calls `dealDamageToTarget` for each creature, which calls `applyDamagePreventionShields` →
`applyStaticDamageReduction` → creates another `StateProjector` and projects again. For N attacking creatures blocked by
M blockers, this is $O(N \cdot M)$ projections.

**Fix**: Introduce a `ProjectionCache` that's valid for a single state snapshot (keyed by `GameState.timestamp`), or
thread the projected state through method signatures instead of recomputing it.

</details>

<details>
<summary><strong>2. ContinuationHandler is a 1700+ line God class with 30+ resume methods</strong></summary>

`ContinuationHandler.kt` is the single largest file in the project and handles every possible continuation type with
individual `resume*` methods. This creates several problems:

- Every new decision type requires modifying this file
- High merge conflict probability
- Difficult to test individual continuations in isolation
- Violates Open-Closed Principle

**Fix**: Apply the same Strategy pattern used for `EffectExecutor` and `ActionHandler`. Create a
`ContinuationExecutor<T : ContinuationFrame>` interface and `ContinuationExecutorRegistry`, mirroring the existing
handler infrastructure.

</details>

<details>
<summary><strong>3. Hardcoded `StackResolver` instances bypass dependency injection</strong></summary>

Several executors instantiate `StackResolver()` directly instead of using the injected instance:

```kotlin
// CounterSpellExecutor.kt
return StackResolver().counterSpell(state, targetSpell.spellEntityId)

// CounterSpellWithFilterExecutor.kt  
return StackResolver().counterSpell(state, targetSpell.spellEntityId)
```

These fresh instances have `cardRegistry = null`, meaning they can't look up card definitions. If counter-spell
resolution ever needs card data (e.g., for triggered abilities on countered spells), this will silently fail.

**Fix**: Pass `StackResolver` through the executor module constructors, or make the counter-spell executors depend on a
shared instance from the registry.

</details>

<details>
<summary><strong>4. Race condition in floating effect timestamp ordering</strong></summary>

Floating effects use `System.currentTimeMillis()` for timestamps:

```kotlin
timestamp = System.currentTimeMillis()
```

Two effects created in the same millisecond will have identical timestamps, making Layer 613 ordering nondeterministic.
This can cause different results on different runs for the same game state, which is fatal for:

- Replay systems
- Networked games with state verification
- Deterministic testing

**Fix**: Use `GameState.timestamp` (the monotonic tick counter) instead of wall clock time. It's already being
incremented via `state.tick()`.

</details>

---

## 🟠 High Priority (Fix Soon)

<details>
<summary><strong>5. Serialization module is incomplete — missing multiple polymorphic subtypes</strong></summary>

`Serialization.kt` registers polymorphic subtypes for `Component`, `ContinuationFrame`, `PendingDecision`, and
`DecisionResponse`. However, several types from the codebase are missing:

- `MayTriggerContinuation` — not registered in `ContinuationFrame` polymorphic
- `MayPayManaTriggerContinuation` — not registered
- `ManaSourceSelectionContinuation` — not registered
- `AnyPlayerMayPayContinuation` — not registered
- `DistributeDamageContinuation` — not registered
- `ReturnFromGraveyardContinuation` — not registered
- `LookAtOpponentLibraryContinuation` — not registered
- `ReorderOpponentLibraryContinuation` — not registered
- `LookAtTopCardsContinuation` — not registered
- `RevealAndOpponentChoosesContinuation` — not registered
- `PutOnBottomOfLibraryContinuation` — not registered
- `MayPayManaContinuation` — not registered
- `ReorderLibraryDecision` — not registered in `PendingDecision` polymorphic
- `ManaSourcesSelectedResponse` — not registered in `DecisionResponse` polymorphic
- `MustAttackPlayerComponent` — not registered in `Component` polymorphic
- `AdditionalCombatPhasesComponent` — not registered
- `SkipNextTurnComponent` — not registered
- Several game events (`StatsModifiedEvent`, `KeywordGrantedEvent`, `CreatureTypeChangedEvent`, `TurnFaceUpEvent`,
  `TurnedFaceDownEvent`, `ControlChangedEvent`, `CardCycledEvent`, `PermanentsSacrificedEvent`, `ScryCompletedEvent`,
  `LookedAtCardsEvent`, `HandLookedAtEvent`, `HandRevealedEvent`, `CardsRevealedEvent`, `LibraryReorderedEvent`,
  `LoyaltyChangedEvent`) — not registered in `GameEvent` polymorphic

Any attempt to serialize/deserialize game state containing these types will throw a `SerializationException`. This makes
save/load, network transport, and state snapshots broken for a large number of game situations.

**Fix**: Add all missing subtypes. Consider generating this registration via annotation processing or a compile-time
check that all sealed subtypes are registered.

</details>

<details>
<summary><strong>6. Zone model doesn't properly handle control-changing effects for battlefield zones</strong></summary>

The battlefield zone is keyed by `ZoneKey(ownerId, Zone.BATTLEFIELD)`, but control-changing effects (Annex, Threaten)
only modify the `ControllerComponent` and create floating effects. The card stays in the original owner's battlefield
zone:

```kotlin
// ZoneKey(originalOwner, BATTLEFIELD) still contains the permanent
// But projected controller says a different player controls it
```

This means `state.getBattlefield(playerId)` returns cards the player **owns**, not cards they **control**. The code
compensates by using `stateProjector.project(state).getBattlefieldControlledBy(playerId)` in some places but uses the
raw zone in others — inconsistently. For example, `SacrificeExecutor.findValidPermanents` correctly uses projected
state, but `ForceSacrificeExecutor.findValidPermanents` uses `ZoneKey(playerId, Zone.BATTLEFIELD)` directly.

**Fix**: Either (a) move permanents between battlefield zones when control changes, or (b) always use projected state
for "controlled by" queries and add a lint/convention to prevent direct zone access for battlefield.

</details>

<details>
<summary><strong>7. No protection against infinite loops in SBA checking</strong></summary>

`StateBasedActionChecker.checkAndApply()` loops until no SBAs apply:

```kotlin
do {
    val result = checkOnce(currentState)
    actionsApplied = result.events.isNotEmpty()
    // ...
} while (actionsApplied)
```

If a replacement effect or component interaction causes SBAs to perpetually generate events, this loops forever. While
unlikely in normal play, custom cards or bugs in effect executors could trigger this.

**Fix**: Add a maximum iteration count (e.g., 1000) and log a warning/error if exceeded.

</details>

<details>
<summary><strong>8. Aura attachment cleanup is incomplete</strong></summary>

When a permanent leaves the battlefield, `stripBattlefieldComponents` removes `AttachedToComponent` and
`AttachmentsComponent` from the entity. However, it doesn't update other entities that reference the removed entity:

- If Creature A has `AttachmentsComponent(listOf(AuraB))` and AuraB is destroyed, nothing removes AuraB from Creature
  A's `AttachmentsComponent`
- If AuraB has `AttachedToComponent(CreatureA)` and CreatureA dies, the SBA checker handles this for auras. But
  equipment and other attachments could have stale references.

The `AttachmentsComponent` appears to not be used anywhere except as a data definition — nothing reads it or maintains
it.

**Fix**: Either remove `AttachmentsComponent` if it's unused, or implement bidirectional attachment tracking
consistently.

</details>

---

## 🟡 Medium Priority (Plan for Next Iteration)

<details>
<summary><strong>9. EffectExecutorUtils is a static utility bag violating dependency injection</strong></summary>

`EffectExecutorUtils` creates its own `StateProjector()` and `PredicateEvaluator()` instances:

```kotlin
object EffectExecutorUtils {
    private val stateProjector = StateProjector()
    private val predicateEvaluator = PredicateEvaluator()
```

This means these singleton instances can't be shared with or configured by the rest of the system. It also makes testing
harder since you can't mock the projector.

**Fix**: Convert to a class with injected dependencies, or pass the projector as a parameter to methods that need it.

</details>

<details>
<summary><strong>10. Dual result types create confusion — ExecutionResult vs EngineResult</strong></summary>

The codebase has two parallel result types:

- `ExecutionResult` (legacy, used everywhere)
- `EngineResult` (new sealed interface, used only in `MulliganHandler`)

`ExecutionResult` has a `toEngineResult()` converter and `fromEngineResult()` factory, but the conversion is lossy (
e.g., `EngineResult.GameOver` maps to `GameEndReason.UNKNOWN`). The mulligan handlers convert between the two at every
boundary:

```kotlin
return when (val result = mulliganHandler.handleKeepHand(state, action)) {
    is EngineResult.Success -> checkMulliganCompletion(result.newState, result.events)
    is EngineResult.Failure -> ExecutionResult.error(result.originalState, result.message)
    // ...
}
```

**Fix**: Pick one and migrate. `EngineResult` is the better design (sealed interface, exhaustive when), but requires a
coordinated migration.

</details>

<details>
<summary><strong>11. No validation that continuation stack is empty when game ends</strong></summary>

If a game ends (via concession, life loss, etc.) while there are unprocessed continuation frames on the stack, those
frames are silently abandoned. This could cause issues if:

- Game state is serialized and later inspected
- A replay system tries to reconstruct the game

**Fix**: Clear the continuation stack when `gameOver = true` is set, or at minimum assert it's empty in tests.

</details>

<details>
<summary><strong>12. Legend rule implementation doesn't ask the player which copy to keep</strong></summary>

Per MTG rules (704.5j), when a player controls two legendary permanents with the same name, **the player chooses** which
one to keep. The current implementation always keeps the first one:

```kotlin
// Keep first, sacrifice rest
for (i in 1 until entityIds.size) {
```

**Fix**: Create a `SelectCardsDecision` for the player to choose which legendary permanent to keep.

</details>

<details>
<summary><strong>13. ManaSolver doesn't account for dual-producing sources correctly in all cases</strong></summary>

The `findBestSourceForColor` method selects sources based on tap priority, but doesn't consider the global optimization
problem. For example, with a cost of $\{U\}\{R\}$ and sources `[Volcanic Island (U/R), Island (U), Mountain (R)]`, the
greedy algorithm might tap Island for U and Mountain for R, leaving Volcanic Island untapped. But if you needed Volcanic
Island for a different spell, this would be suboptimal.

This is fundamentally an NP-hard problem (bin packing variant), but the current greedy approach could be improved with a
two-pass approach: assign constrained colors first, then flexible ones.

**Fix**: Consider a constraint-satisfaction approach for mana solving, or at minimum handle the "exactly one source
produces this color" case before general assignment.

</details>

<details>
<summary><strong>14. CardComponent stores both ownerId and there's a separate OwnerComponent</strong></summary>

`CardComponent` has `val ownerId: EntityId?` and entities also have `OwnerComponent(playerId: EntityId)`. Some code
reads one, some reads the other, and they can theoretically disagree:

```kotlin
val ownerId = container.get<OwnerComponent>()?.playerId
    ?: cardComponent.ownerId
    ?: controllerId
```

This defensive chaining appears in many places and suggests the data model has redundancy.

**Fix**: Remove `ownerId` from `CardComponent` and use `OwnerComponent` exclusively, or vice versa.

</details>

<details>
<summary><strong>15. Trigger detection uses "look back in time" but only for simultaneous deaths</strong></summary>

`detectSimultaneousDeathTriggers` handles Rule 603.10 for creatures dying at the same time, but other "look back in
time" scenarios aren't covered:

- A creature's leaves-the-battlefield trigger should see the game state as it was before the creature left
- Sacrifice-as-cost triggers should see the sacrificed creature's last known information

The current implementation checks `state.getEntity(deadEntityId)` which works because entities aren't removed from the
entity map when they change zones — but this is fragile and undocumented.

**Fix**: Document the invariant that entities persist in the entity map even after zone changes, or implement proper
last-known-information tracking.

</details>

---

## 🟢 Low Priority (Nice to Have)

<details>
<summary><strong>16. Missing unit test infrastructure for continuations</strong></summary>

The continuation system is the most complex part of the engine (30+ continuation types, each with specific validation
and state mutation logic), but there's no visible test infrastructure. Each continuation type should have dedicated
tests verifying:

- Correct state mutation on valid input
- Error handling on invalid input
- Interaction with `checkForMoreContinuations`
- Serialization round-trip

</details>

<details>
<summary><strong>17. ScryExecutor is a stub</strong></summary>

```kotlin
class ScryExecutor : EffectExecutor<ScryEffect> {
    override fun execute(...): ExecutionResult {
        return ExecutionResult.success(state.tick())
    }
}
```

Scry is a common mechanic that doesn't actually work.

</details>

<details>
<summary><strong>18. Several TODO comments indicate incomplete features</strong></summary>

Scattered throughout the code:

- `ConditionEvaluator`: 6 TODO conditions returning `false`
- `PredicateEvaluator.matchesStatePredicate`: `EnteredThisTurn`, `WasDealtDamageThisTurn`, `HasDealtDamage`,
  `HasDealtCombatDamageToPlayer` all return `false`
- `CostHandler`: Discard and ExileFromGraveyard costs have placeholder implementations

These represent cards that will silently malfunction.

</details>

<details>
<summary><strong>19. Inconsistent error handling philosophy</strong></summary>

Some executors return `ExecutionResult.success(state)` when a target is missing (graceful fizzle), while others return
`ExecutionResult.error(state, "...")`. The MTG rules generally say "do as much as possible" — so most effects should
fizzle gracefully rather than error.

```kotlin
// Graceful (correct for most effects):
?: return ExecutionResult.success(state)

// Error (may be wrong):  
    ?: return ExecutionResult.error(state, "No valid target for damage")
```

**Fix**: Establish a convention: effects fizzle gracefully (`success`), validation failures error. Document this.

</details>

<details>
<summary><strong>20. GameState serialization will be very large</strong></summary>

`GameState` contains the full entity map with all component data, all zones, all floating effects, and the full
continuation stack. For a mid-game state with 40+ entities, this could be several hundred KB of JSON. Network games
sending this per-action will have bandwidth issues.

**Fix**: Consider delta-based state updates for network transport, while keeping full snapshots for save/load.

</details>

<details>
<summary><strong>21. No logging or observability hooks</strong></summary>

The engine is entirely silent — no logging, no metrics, no tracing. When something goes wrong in production, the only
diagnostic is the error string in `ExecutionResult`. Adding structured logging at action boundaries and continuation
resumptions would significantly improve debuggability.

</details>

<details>
<summary><strong>22. Hand smoother algorithm bias</strong></summary>

The hand smoother uses `library.shuffled().takeLast(count)` for candidates:

```kotlin
val shuffledLibrary = library.shuffled()
val candidateHand = shuffledLibrary.takeLast(count)
```

Using `takeLast` instead of `take` creates a subtle bias — the cards that end up at the end of the shuffled list are
always the hand candidates, and the "remaining library" order is always top-to-bottom of the same shuffle. This doesn't
affect fairness (it's still random), but `take` would be more conventional and clearer.

</details>

---

## Summary by Priority

| Priority    | Count | Theme                                                                          |
|-------------|-------|--------------------------------------------------------------------------------|
| 🔴 Critical | 4     | Performance, God class, DI bypass, nondeterminism                              |
| 🟠 High     | 4     | Serialization gaps, zone model inconsistency, infinite loops, stale references |
| 🟡 Medium   | 7     | Utility objects, dual result types, rules correctness, data model redundancy   |
| 🟢 Low      | 7     | Test coverage, stubs, TODOs, conventions, logging                              |