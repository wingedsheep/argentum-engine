# `:engine-gym-trainer`

JVM-side self-play, MCTS, and training-data generation for the Argentum
engine. Sits between [`:engine-gym`](../engine-gym/README.md) (the env
runtime) and your NN inference code.

If you're writing an AlphaZero-shaped agent, an MCTS-based search bot, or
any Reinforcement-Learning loop that needs Magic game states, **this is the
module you plug into**. You supply the neural network (typically as a
remote inference server); we supply the tree search, the self-play driver,
and the data-writing plumbing.

## Why a separate module

`:engine-gym` is transport-agnostic and decision-aware: it knows how to
reset, step, fork, snapshot and restore games. That's enough for an agent
that does one MCTS walk per move, but it deliberately stops short of
providing MCTS, feature encoding, or training-data formats — those are
choices a training project should own.

`:engine-gym-trainer` is where the RL-shaped opinions live:

- An **AlphaZero-style MCTS** that uses `GameEnvironment.fork()` for
  free tree expansion.
- A tiny **SPI** — four small interfaces — that separates what the trainer
  decides (tree search, temperature schedule, back-patching outcome
  labels) from what a project decides (feature representation, action
  encoding, NN inference, on-disk format).
- Reasonable **defaults** so a training loop runs end-to-end with zero NN
  setup — a heuristic evaluator, a structural featurizer, a JSONL sink.

It is a pure Kotlin library — no Spring, no HTTP server. If you want an
HTTP transport in front of the env itself, see
[`:engine-gym-server`](../engine-gym-server/README.md). If your trainer
is Python-only and wants to drive the engine over HTTP, `:engine-gym-server`
is what you want and you can skip this module.

## The four SPI traits

```kotlin
// 1. Engine state → feature vector your NN consumes
fun interface StateFeaturizer<T> {
    fun featurize(ctx: TrainerContext): T
}

// 2. Engine action → stable (head, slot) pair on your policy
interface ActionFeaturizer {
    val heads: List<PolicyHead>
    fun slot(action: GameAction, ctx: TrainerContext): SlotEncoding
}

// 3. Priors + value; typically a remote NN call
fun interface Evaluator<T> {
    fun evaluate(features: T, legalSlots: List<SlotEncoding>, ctx: TrainerContext): EvaluationResult
}

// 4. Self-play data writer
interface SelfPlaySink<T> : AutoCloseable {
    fun beginGame(gameId: String, players: List<EntityId>)
    fun recordStep(features: T, ctx: TrainerContext, actingPlayer: EntityId,
                   headUsed: String, legalSlots: List<SlotEncoding>,
                   visits: IntArray, mctsValue: Float)
    fun endGame(winner: EntityId?)
}
```

The only shared piece is `TrainerContext` — the state, the acting player,
and the engine's `PendingDecision` (non-null when mid-decision). Every SPI
call receives it so implementations can branch on decision kind (priority
vs target selection vs yes/no vs …).

## Design choices worth knowing about

### Multi-head policy is first-class

`ActionFeaturizer.heads` lets the network expose any number of disjoint
policy heads. An AlphaZero-for-chess setup passes one head with 4,672
slots; an MTG setup that wants separate priority / target / binary heads
passes three. The engine-side MCTS treats them uniformly — it just reads
`priors[head][slot]` for every legal edge.

Why multi-head instead of flat? MageZero, the reference AlphaZero-for-MTG
project this was designed to fit, uses four heads (priority-player /
priority-opponent / target / binary). Forcing every project through a
single flat head would have made them fork, which was the whole problem
we were trying to solve. The cost for simple users is a one-element
`listOf(PolicyHead("actions", 128))`.

### Engine-side MCTS, not Python-side

Tree search lives in the JVM. The Python side of a typical setup is only
an inference server — features in, priors+value out. This is the model
MageZero and most AlphaZero MTG projects use because tree expansion is
memory-hot, needs `GameState.fork()` to be cheap, and doesn't parallelise
well across HTTP round-trips.

If your architecture is Python-driven instead (Python owns the tree, JVM
is just a state advance), use [`:engine-gym-server`](../engine-gym-server/README.md)'s
HTTP surface and skip this module.

### Decision-aware edges

MCTS edges are ordinary `GameAction`s. At a priority state, edges are
`LegalAction`s. At a simple pending decision (yes/no, choose-number, and
friends), edges are `SubmitDecision(response)` — exactly the set of
folded responses `:engine-gym`'s `ActionRegistry` produces. At a complex
pending decision (targets, distribute, order, search, etc.) the
`StructuredDecisionResolver` returns a single forced edge. The built-in
resolver samples uniformly; a production project can supply a heuristic
or learned one.

### Outcome-labelled self-play rows

`SelfPlaySink` buffers rows per game and back-patches the terminal
outcome onto every row before flushing. So the rows you persist are
already the `(state, π, z)` triples AlphaZero training expects — no
second pass needed.

### No server, no Python, no ONNX

The library only depends on `:engine-gym`, `:rules-engine`, `:mtg-sdk`
and kotlinx-serialization. There's no HTTP server here and no embedded
ONNX runtime; those are explicit downstream choices and they shouldn't
bloat everyone's classpath.

## What's provided out of the box

| Built-in | Purpose |
|---|---|
| `AlphaZeroSearch` | PUCT + optional Dirichlet root noise + free `fork()` expansion |
| `SelfPlayLoop` | Drives a game with temperature schedule, labels rows with terminal outcome |
| `HeuristicEvaluator` | No-NN evaluator — uses the existing engine `BoardEvaluator`. Good for loop plumbing and warm starts. |
| `RemoteHttpEvaluator` | JSON-over-HTTP to a Python inference server. Swap codec (MessagePack / Protobuf) by subclassing. |
| `StructuralStateFeaturizer` | Simple `Map<String, Float>` from life, zone sizes, projected P/T totals, mana. Replace for real training. |
| `DynamicSlotActionFeaturizer` | Single-head, hash-keyed slot assignment. Replace for serious training to avoid collisions. |
| `JsonlSelfPlaySink` | One JSON line per step, outcome label included. Easy Python ingest. |
| `RandomStructuredResolver` | Uniform-random response for structured decisions (targets, etc.). |

Defaults exist so a training loop runs in 30 lines. Every one is intended
to be replaced when a project gets serious.

## Hello world (no NN)

```kotlin
val registry = CardRegistry().apply { register(PortalSet.allCards) }
val sink = JsonlSelfPlaySink(
    path = Path.of("data/games.jsonl"),
    featureSerializer = StructuralFeatures.serializer()
)

val loop = SelfPlayLoop(
    envFactory = { GameEnvironment.create(registry) },
    featurizer = StructuralStateFeaturizer(),
    actionFeaturizer = DynamicSlotActionFeaturizer(headSize = 1024),
    evaluator = HeuristicEvaluator(),
    sink = sink,
    simulationsPerMove = 32,
    dirichletAlpha = 0.3
)

loop.playGames(count = 100) {
    GameConfig(
        players = listOf(
            PlayerConfig("A", Deck.of("Mountain" to 17, "Raging Goblin" to 3)),
            PlayerConfig("B", Deck.of("Mountain" to 17, "Raging Goblin" to 3))
        ),
        skipMulligans = true
    )
}
sink.close()
```

## Bringing a real NN

Point a `RemoteHttpEvaluator` at your Python inference endpoint. The wire
contract is minimal:

```
POST /evaluate
{
  "features": "<featuresJson>",
  "legalSlots": [{"head": "actions", "slot": 3}, ...],
  "decision": {"isPriority": true, "playerId": "..."}
}
→
{
  "priors": {"actions": [0.1, 0.2, ...]},
  "value": 0.25
}
```

If your network uses a different codec, subclass `RemoteHttpEvaluator`
and override `evaluate`. The SPI itself is four lines — writing a custom
`Evaluator` from scratch is trivial.

## Scope — what's *not* here

- **No NN runtime.** No PyTorch, no ONNX, no JAX. The trainer sends
  features to whatever you run the network in.
- **No HDF5 / NPZ sinks.** Add your own `SelfPlaySink` implementation; the
  interface is eight lines.
- **No sparse-hashed featurizer.** A `WeinbergerSparseFeaturizer` contrib
  module is a natural addition if/when it's needed.
- **No multi-process worker pool.** `:engine-gym`'s `MultiEnvService.stepBatch`
  already parallelises at the env level; if you need distributed rollouts,
  orchestrate at the Python training script level.

## Tests

```bash
just test-gym-trainer              # this module only
./gradlew :engine-gym-trainer:test # same via gradle
```

The test set covers:
- PUCT visit accounting (`AlphaZeroSearchTest`)
- Root noise doesn't break the search
- End-to-end self-play producing outcome-labelled JSONL (`SelfPlayLoopTest`)
