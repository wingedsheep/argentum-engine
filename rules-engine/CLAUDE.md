# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test

```bash
just test-rules                                    # Run all rules-engine tests
just test-class CreatureStatsTest                  # Run specific test class
./gradlew :rules-engine:test --tests "MyTest"      # Gradle equivalent
```

## Module Overview

The rules-engine is a **pure Kotlin library** — no Spring, no I/O. All operations are
`(GameState, GameAction) -> ExecutionResult`. It depends only on `mtg-sdk` (card definitions, effects,
costs). Card-specific logic lives in `mtg-sets`; the engine knows nothing about specific cards.

## Package Structure

```
com.wingedsheep.engine
├── core/          # Entry points: ActionProcessor, TurnManager, GameInitializer
├── state/         # GameState + ComponentContainer; all component types under state/components/
├── event/         # TriggerDetector, TriggerProcessor
├── handlers/
│   ├── actions/   # ActionHandlerRegistry + per-action handlers (spell/, combat/, etc.)
│   ├── effects/   # EffectExecutorRegistry + per-effect executors (damage/, drawing/, etc.)
│   └── continuations/ # ContinuationHandler + per-type resumer modules
└── mechanics/
    ├── layers/    # StateProjector (Rule 613), ProjectedState
    ├── combat/    # CombatManager
    ├── mana/      # ManaSolver, CostCalculator
    ├── replacement/ # ReplacementEffectChecker
    └── StateBasedActionChecker
```

## Core Execution Flow

```
Client action
  → ActionProcessor.process()
  → ActionHandlerRegistry  (finds handler by KClass)
  → Handler.validate() then .execute()
  → PostActionProcessor.processTriggersAndReturnPriority()
      → TriggerDetector.detectTriggers(state, events)
      → TriggerProcessor.processTriggers()
  → ExecutionResult (success | paused | error)

If paused: client submits decision
  → ContinuationHandler.resume()
  → pops ContinuationFrame from state.continuationStack
  → resumes matching execution path
  → checkForMoreContinuations() drains any stacked continuations
```

## Key Patterns

### Adding a new action type
1. Add case to `core/GameAction.kt` (sealed interface)
2. Implement `ActionHandler<YourAction>` (has `actionType`, `validate`, `execute`)
3. Register in an `ActionHandlerModule`, add module to `ActionProcessor`

### Adding a new effect type
Effects are defined in `mtg-sdk` — the engine only implements executors:
1. Add effect class in mtg-sdk
2. Implement `EffectExecutor<YourEffect>` in `handlers/effects/`
3. Register in an `ExecutorModule`, add module to `EffectExecutorRegistry`

### Adding a new continuation type
1. Add case to `core/Continuation.kt` (sealed interface)
2. Add resume branch in `ContinuationHandler` (or a new resumer module)

### Continuous effects (Rule 613)
Never mutate base components. Instead create a `ContinuousEffect` that `StateProjector` applies
during projection. Within each layer, effects are sorted by **dependency** (trial application per
Rule 613.8), then by timestamp. When two `ContinuousEffect` data class instances are structurally
equal (e.g., two identical lord bonuses), use a list — not a set — for storage: `toMutableList()`
not `toMutableSet()`.

### Trigger detection
- Battlefield triggers detected in `TriggerDetector.detectTriggers()` per event
- Phase/step triggers detected in `detectPhaseStepTriggers()` — called by `PassPriorityHandler`
  on `StepChangedEvent` (these never match in `matchesTrigger()`)
- Permanents that leave the battlefield require `detectLeavesBattlefieldTriggers()` since they're
  no longer in `getBattlefield()` when the event fires

### Trigger ordering (APNAP)
`TriggerProcessor.processTriggers()` handles one trigger at a time. If the first trigger pauses for
target selection, remaining triggers are stored in `PendingTriggersContinuation` on the stack and
resumed by `ContinuationHandler.checkForMoreContinuations()` after each trigger resolves.

### MayEffect + targeted triggers
Order: yes/no decision first, then target selection. Test flow:
`resolveStack()` → `answerYesNo(true)` → `selectTargets()` → `resolveStack()`

### Continuations and targets
Any `ContinuationFrame` that wraps an effect using `EffectTarget.ContextTarget(n)` must include
the `targets` list. If you add a new continuation type that executes effects, propagate targets from
the continuation into `EffectContext`.

## Testing

Tests use **Kotest FunSpec**. Use `GameTestDriver` for unit/integration tests — it wraps
`ActionProcessor` with helper methods (`playLand`, `castSpell`, `passPriority`, `declareAttackers`,
etc.) and query methods (`getHand`, `getLifeTotal`, `getCreaturesOnBattlefield`).

Scenario tests live under `src/test/kotlin/.../scenarios/` and use `ScenarioTestBase`.

```kotlin
class MyTest : FunSpec({
    test("description") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Grizzly Bears" to 20))
        // drive game, assert state
    }
})
```

## Projected vs Base State

**Always read projected state** for game logic (P/T, controller, types, keywords). Base components
hold raw data; `StateProjector` calculates what the game actually sees. Several past bugs came from
reading base `ControllerComponent` instead of projected state — use `projectedState.getController()`
or `ActionContext.projectedState`.
