# `:gym`

A stateful Gymnasium-style environment wrapping the rules engine. Built
for agent research — AlphaZero, MCTS, policy-gradient RL, or simple
heuristic bots.

This is the JVM runtime layer. It's transport-agnostic (no HTTP, no
Spring, no sockets), decision-aware, and exposes a small `reset` / `step`
/ `observe` / `fork` API that any higher layer — an HTTP server, a
Python-driven trainer, a JVM-side MCTS — can drive.

## Why this module exists

The rules-engine is a pure function: `(GameState, GameAction) ->
ExecutionResult`. Perfect for correctness, awkward for training loops
that want to say "take this action, give me the next state, sample
legal actions, fork a hypothetical, roll back."

`:gym` does that wrapping once so every downstream RL adapter
doesn't have to:

- **Mutable env object** that remembers state, playerIds, turn number.
- **O(1) fork** for MCTS tree expansion — `GameState` is immutable, so
  two envs share structure for free.
- **Snapshot/restore** for rollouts from a saved position.
- **Legal-action enumeration** including the folding of simple pending
  decisions into the same action-ID space.
- **`MultiEnvService`** — an in-JVM registry that manages many envs, runs
  `stepBatch` in parallel, and is the thing an HTTP transport or a trainer
  actually talks to when it owns thousands of concurrent games.
- **`TrainingObservation`** — a stable JSON-serialisable snapshot of
  everything an agent needs to see, with information-hiding (opponent hand
  and libraries masked by default), a schema hash for fail-fast contract
  checks, and a state digest usable as an MCTS transposition key.

## The two entry points

**Direct — `GameEnvironment`.** One env, single-threaded, cheap to fork.
Use this from a JVM-side trainer that owns its own loop.

```kotlin
val env = GameEnvironment.create(cardRegistry)
env.reset(GameConfig(players = ..., skipMulligans = true))

while (!env.isTerminal) {
    val actions = env.legalActions()
    val chosen = actions.random()
    env.step(chosen.action)
}
println("Winner: ${env.winnerId}")

// MCTS: fork is free
val child = env.fork()
child.step(someAction)
```

**Managed — `MultiEnvService`.** Registry of many envs keyed by `EnvId`.
Intended for servers that host thousands of envs for external trainers.

```kotlin
val svc = MultiEnvService(cardRegistry, boosterGenerator)
val (envId, opening) = svc.create(EnvConfig(players = ...))
svc.step(StepRequest(envId, actionId = 0))
svc.stepBatch(listOf(StepRequest(a, 0), StepRequest(b, 1)))  // parallel per-env
val snap = svc.snapshot(envId)
svc.restore(envId, snap)
svc.dispose(listOf(envId))
```

Both APIs share the same `GameState`, `LegalAction`, and `PendingDecision`
types — there's no parallel type hierarchy.

## Design choices worth knowing about

### Action IDs are per-step, not stable

`ActionRegistry` assigns integer IDs to legal actions / folded decisions
for the current observation. **They are regenerated on every `step` /
`reset` and should not be cached across steps.** A training loop fetches
IDs from one observation and uses them immediately; if an old ID leaks
into a later step, `MultiEnvService.step` throws `IllegalArgumentException`
(which `:gym-server` maps to a 400).

Why not stable? Because MTG's action space is dynamic — different games
expose different actions, and the same position at different times
exposes a different legal set. Making IDs stable across steps would have
required an up-front enumeration of the full action space, which neither
scales across sets nor survives custom card sets.

### Simple decisions fold into the action space

When the engine pauses on a `YesNoDecision`, `ChooseNumberDecision`,
single-mode `ChooseModeDecision`, `ChooseColorDecision`,
`ChooseOptionDecision`, or single-select `SelectCardsDecision`, the
`ActionRegistry` enumerates every concrete `DecisionResponse` into the
same ID space. A trainer doesn't need a separate code path for "yes/no";
it just steps by ID like any other action.

Complex decisions (`ChooseTargetsDecision`, `DistributeDecision`,
`OrderObjectsDecision`, `SplitPilesDecision`, `SearchLibraryDecision`,
`ReorderLibraryDecision`, `AssignDamageDecision`,
`SelectManaSourcesDecision`, multi-select `SelectCardsDecision`,
multi-mode `ChooseModeDecision`, `BudgetModalDecision`) flag
`requiresStructuredResponse = true` and need a purpose-built
`DecisionResponse` submitted via `MultiEnvService.submitDecision`.

### Information hiding by default

`ObservationBuilder` hides opponent hand and every library when building
a `TrainingObservation`. Only zone sizes are reported for hidden zones.
A `revealAll = true` flag is available for debug tooling and must not be
enabled in real self-play (the agent would be training on leaked
information).

### State digest for transposition tables

Every `TrainingObservation` carries a `stateDigest` — a SHA-256 hash of
the observable state. Identical observations produce identical digests;
stepping once changes the digest. Useful as an MCTS transposition key or
as a cheap cache key for distributed rollouts.

### Immutable state, O(1) fork, O(1) snapshot

`GameEnvironment.fork()` returns a new env pointing at the same
`GameState` reference — no deep copy. `MultiEnvService.snapshot()` stores
the same reference in a slot. `restore()` writes the reference back. All
three are constant-time; the only cost is object allocation.

## What's *not* here

- **No HTTP/WebSocket transport.** See
  [`:gym-server`](../gym-server/README.md).
- **No MCTS.** See [`:gym-trainer`](../gym-trainer/README.md).
- **No NN inference.** The env is agnostic to featurization.
- **No training-data persistence.** `:gym-trainer` handles this;
  `:gym` only emits live observations.
- **No Spring, no threading model assumptions.** Envs are
  single-threaded by contract; cross-env parallelism is opt-in via
  `MultiEnvService.stepBatch`.

## Tests

```bash
just test-gym                   # this module only
./gradlew :gym:test      # same via gradle
```

35 tests across:

- `GameEnvironmentTest` — core reset/step/fork/evaluate/playGame.
- `TrainingObservationTest` — observation contract, masking, digest stability.
- `MultiEnvServiceTest` — lifecycle, step-batch, fork/snapshot/restore,
  deck validation, perspective masking.

## When to use which module

- Python trainer owns the loop → [`:gym-server`](../gym-server/README.md)
  (HTTP shell) + `:gym` transitively.
- JVM trainer with a Python-side NN → `:gym` + [`:gym-trainer`](../gym-trainer/README.md).
- Pure-JVM agent, no Python at all → `:gym` + `:gym-trainer`
  with a local-evaluator `Evaluator` implementation.
- Just want to replay a game or run one turn → `:gym` alone.
