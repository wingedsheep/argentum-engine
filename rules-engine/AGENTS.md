# rules-engine

Pure Kotlin library — no Spring, no I/O. All operations are
`(GameState, GameAction) -> ExecutionResult`. Depends only on `mtg-sdk`. Knows nothing about
specific cards.

## Build & Test

```bash
just test-rules                                    # All rules-engine tests
just test-class CreatureStatsTest                  # Specific test class
./gradlew :rules-engine:test --tests "MyTest"      # Gradle equivalent
```

## Where to look

- **Core design** (ECS, continuations, layer system, mana, priority): [`../docs/architecture-principles.md`](../docs/architecture-principles.md)
- **Rule 613.8 dependency resolution**: [`../docs/continuous-effect-dependency-system.md`](../docs/continuous-effect-dependency-system.md)
- **Complex/rare ability patterns**: [`../docs/managing-complex-and-rare-abilities.md`](../docs/managing-complex-and-rare-abilities.md)
- Package layout under `com.wingedsheep.engine/` — read the directory.

## Load-bearing rules (engine-specific, beyond root AGENTS.md)

- **Trigger detection has three entry points**, not one:
  - `TriggerDetector.detectTriggers()` — battlefield triggers, per event
  - `detectPhaseStepTriggers()` — phase/step triggers; called by `PassPriorityHandler` on
    `StepChangedEvent`. These never match in `matchesTrigger()`.
  - `detectLeavesBattlefieldTriggers()` — permanents that left the battlefield aren't in
    `getBattlefield()` anymore, so dies/leaves triggers need this separate pass.
- **Mid-resolution pauses must preserve queued triggers.** `TriggerProcessor.processTriggers()`
  handles one trigger at a time; if it pauses, remaining triggers go on
  `PendingTriggersContinuation` and `ContinuationHandler.checkForMoreContinuations()` resumes them.
- **Continuations that execute effects must propagate `targets`.** Any new `ContinuationFrame`
  wrapping an effect with `EffectTarget.ContextTarget(n)` has to include the targets list, or the
  effect resolves with an empty context and silently fizzles.
- **Continuous effect storage is a list, not a set.** Two structurally-equal `ContinuousEffect`
  instances (e.g., two identical lord bonuses from different sources) must both be applied — use
  `toMutableList()` in `StateProjector.sortByDependencyAndTimestamp`, never `toMutableSet()`.
