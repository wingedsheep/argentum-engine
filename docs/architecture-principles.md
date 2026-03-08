# Architectural Principles of the Argentum Engine

This document explains the core architectural principles behind the Argentum Engine, a Magic: The Gathering
rules engine and online play platform. Each section covers a different part of the application, the key
design decisions made, and the reasoning behind them.

---

## Table of Contents

1. [SDK: The Data Contract Layer](#1-sdk-the-data-contract-layer)
   - [1.1 Data-Driven Card Definitions](#11-data-driven-card-definitions)
   - [1.2 Abstract Syntax Trees for Dynamic Values](#12-abstract-syntax-trees-for-dynamic-values)
   - [1.3 Composable Filtering](#13-composable-filtering)
   - [1.4 Late-Binding Targets and Variables](#14-late-binding-targets-and-variables)
   - [1.5 Atomic Effect Pipelines](#15-atomic-effect-pipelines)
   - [1.6 DSL as Abstraction Layer](#16-dsl-as-abstraction-layer)
2. [Rules Engine: The Functional Core](#2-rules-engine-the-functional-core)
   - [2.1 Pure Functional State Transitions](#21-pure-functional-state-transitions)
   - [2.2 Entity-Component-System (ECS)](#22-entity-component-system-ecs)
   - [2.3 Rule 613: Base State vs. Projected State](#23-rule-613-base-state-vs-projected-state)
   - [2.4 Reentrant Continuations](#24-reentrant-continuations)
   - [2.5 Explicit Event Emission](#25-explicit-event-emission)
   - [2.6 Strategy-Based Registries](#26-strategy-based-registries)
   - [2.7 Replacement Effects](#27-replacement-effects)
   - [2.8 State-Based Actions](#28-state-based-actions)
3. [Game Server: The Orchestration Layer](#3-game-server-the-orchestration-layer)
   - [3.1 Thin Server, Zero Game Logic](#31-thin-server-zero-game-logic)
   - [3.2 State Masking (Fog of War)](#32-state-masking-fog-of-war)
   - [3.3 Anti-Corruption Layer (DTO Transformation)](#33-anti-corruption-layer-dto-transformation)
   - [3.4 Server-Authoritative Legal Actions](#34-server-authoritative-legal-actions)
   - [3.5 Auto-Pass Priority System](#35-auto-pass-priority-system)
   - [3.6 Tournament System](#36-tournament-system)
4. [Web Client: The Dumb Terminal](#4-web-client-the-dumb-terminal)
   - [4.1 No Game Logic in the Client](#41-no-game-logic-in-the-client)
   - [4.2 Server-Driven Interactivity](#42-server-driven-interactivity)
   - [4.3 Zustand Slice Architecture](#43-zustand-slice-architecture)
   - [4.4 Intent Capture, Not Computation](#44-intent-capture-not-computation)
5. [Testing: Multi-Layered Verification](#5-testing-multi-layered-verification)
   - [5.1 Unit and Integration Tests](#51-unit-and-integration-tests)
   - [5.2 Scenario Tests](#52-scenario-tests)
   - [5.3 End-to-End Tests with Dev Scenario API](#53-end-to-end-tests-with-dev-scenario-api)

---

## 1. SDK: The Data Contract Layer

The `mtg-sdk` module is the shared contract between card content (`mtg-sets`) and execution logic
(`rules-engine`). It defines *what* happens — the engine defines *how*. This separation is the most
important architectural boundary in the system.

### 1.1 Data-Driven Card Definitions

**Principle:** Cards are pure, serializable data structures with zero execution logic.

A card definition like Ancestral Memories contains no `execute()` method, no lambda, no reference to
engine internals:

```kotlin
val AncestralMemories = card("Ancestral Memories") {
    manaCost = "{2}{U}{U}{U}"
    typeLine = "Sorcery"

    spell {
        effect = EffectPatterns.lookAtTopAndKeep(count = 7, keepCount = 2)
    }
}
```

The `effect` field is a `CompositeEffect` — a serializable data tree describing "gather the top 7 cards,
let the player pick 2, move those to hand, move the rest to graveyard." The engine interprets this data
at resolution time.

**Why this matters:**

- **Safe state projection.** The engine needs to calculate what the game "looks like" after applying all
  continuous effects (Rule 613). If card abilities contained executable code, projecting state could
  accidentally trigger side effects. Pure data is safe to inspect.
- **Serialization.** Card definitions can be serialized to JSON, sent over the network for multiplayer,
  stored in databases, or hot-reloaded without recompiling. Lambdas and function references cannot.
- **Testability.** Card scripts are inspectable data — you can write assertions against the structure
  of an effect tree without executing it.
- **Decoupling.** Card authors (in `mtg-sets`) never need to understand engine internals. They compose
  effects from a well-defined vocabulary. Engine developers can refactor execution logic without touching
  any card definition.

### 1.2 Abstract Syntax Trees for Dynamic Values

**Principle:** Numbers are represented as inspectable AST nodes, not integers or lambdas.

Many Magic cards reference dynamic values — Tarmogoyf's power equals the number of card types in all
graveyards, Blaze deals X damage where X is the mana paid. These are modeled as a `DynamicAmount`
sealed hierarchy:

```kotlin
sealed interface DynamicAmount {
    data class Fixed(val amount: Int) : DynamicAmount
    data object XValue : DynamicAmount
    data object CardTypesInAllGraveyards : DynamicAmount
    data class Add(val left: DynamicAmount, val right: DynamicAmount) : DynamicAmount
    data class Subtract(val left: DynamicAmount, val right: DynamicAmount) : DynamicAmount
    data class VariableReference(val variableName: String) : DynamicAmount
    // ... more variants
}
```

A card like Blaze uses `DynamicAmount.XValue` instead of the integer `0`:

```kotlin
effect = DealDamageEffect(DynamicAmount.XValue, target)
```

**Why not just use lambdas like `(GameState) -> Int`?**

- **Serialization.** Lambdas cannot be serialized to JSON or sent over a network. AST nodes can.
- **Inspectability.** The engine can examine *why* a value is what it is. This enables UI text generation
  ("Tarmogoyf's power is equal to the number of card types in all graveyards") and debugging.
- **Lazy evaluation.** The AST is evaluated at the moment the engine needs the value, against the current
  game state. This naturally handles values that change over time without any special caching logic.
- **Composability.** Arithmetic operations like `Add`, `Subtract`, and `Multiply` compose cleanly:
  `DynamicAmount.Add(Fixed(1), Count(...))` is "1 plus the count of matching permanents."

### 1.3 Composable Filtering

**Principle:** One universal filter type replaces hundreds of specific enum cases.

Instead of maintaining separate filter types for every combination of card properties (e.g.,
`TargetRedCreature`, `TargetTappedPermanent`, `ControlledCreatureWithFlying`), the engine uses a single
`GameObjectFilter` that composes three predicate types:

```kotlin
data class GameObjectFilter(
    val cardPredicates: List<CardPredicate> = emptyList(),
    val statePredicates: List<StatePredicate> = emptyList(),
    val controllerPredicate: ControllerPredicate? = null,
    val matchAll: Boolean = true  // true = AND all predicates, false = OR
)
```

This enables fluent construction:

```kotlin
GameObjectFilter.Creature                              // any creature
GameObjectFilter.Creature.withColor(Color.BLACK)       // black creature
GameObjectFilter.Creature.tapped().opponentControls()  // tapped creature opponent controls
GameObjectFilter.Creature.powerAtMost(2)               // creature with power 2 or less
```

**Why this matters:**

- **Combinatorial explosion.** Magic has hundreds of distinct filter combinations across its 20,000+ cards.
  If each were a separate class, the enum would grow unboundedly. Composition reduces N x M specific
  classes to N + M composable predicates.
- **Serialization.** The same `GameObjectFilter` data structure works in card definitions, targeting
  requirements, continuous effect applicability checks, and UI display — all serializable.
- **Reuse.** The same filter predicate logic is used for targeting validation, triggered ability
  conditions, static ability applicability, and forced sacrifice selection.

### 1.4 Late-Binding Targets and Variables

**Principle:** Effects reference targets symbolically, not by concrete entity ID.

When a spell declares "destroy target creature," it doesn't store the creature's entity ID in the effect.
Instead, it uses an `EffectTarget.ContextTarget(0)` — a symbolic reference meaning "the first target
chosen at cast time." Targets are chosen when the spell goes on the stack but *resolved* later when the
effect actually executes.

```kotlin
sealed interface EffectTarget {
    data class ContextTarget(val index: Int) : EffectTarget        // cast-time target by index
    data class BoundVariable(val name: String) : EffectTarget      // cast-time target by name
    data class StoredEntityTarget(val variableName: String) : EffectTarget  // runtime variable
    data object TriggeringEntity : EffectTarget                    // the entity that caused a trigger
    data object Self : EffectTarget                                // the source permanent
    data object Controller : EffectTarget                          // the ability's controller
    // ... more variants
}
```

**Why this matters:**

- **Target legality rechecking.** Magic rules require that targets are rechecked on resolution. If a
  creature gains hexproof between cast and resolution, the spell fizzles. Symbolic references make this
  recheck natural — the engine resolves `ContextTarget(0)` at resolution time and validates it.
- **Oblivion Ring pattern.** `StoredEntityTarget("exiledCard")` elegantly handles cards that exile
  something on ETB and return it on LTB. The leaves-the-battlefield trigger references the exact entity
  exiled by the enters-the-battlefield trigger via a named variable stored in the effect scope, without
  needing closures or mutable state.
- **Serialization.** Symbolic references serialize cleanly. A concrete `EntityId` reference would tie
  the effect to a specific game instance.

### 1.5 Atomic Effect Pipelines

**Principle:** Complex zone manipulations are composed from small, reusable primitives — not monolithic
effect classes.

Instead of creating bespoke effect types for every card that manipulates libraries or zones (which would
require `LookAtTop5Put2InHandRestInGraveyardEffect`, `LookAtTop3Put1OnTopRestOnBottomEffect`, etc.),
the engine uses a functional pipeline model:

```
Gather → Select → Move
```

Three atomic primitives compose into nearly any library or zone manipulation:

| Primitive | Purpose |
|-----------|---------|
| **GatherCardsEffect** | Collect cards into a named collection (no zone change) |
| **SelectFromCollectionEffect** | Present a choice to split a collection |
| **MoveCollectionEffect** | Physically move a named collection to a zone |

Named collections act as variables in an `EffectContext`, flowing data between pipeline stages:

```kotlin
// "Look at the top 7, keep 2, rest to graveyard" = Ancestral Memories
CompositeEffect(listOf(
    GatherCardsEffect(source = CardSource.TopOfLibrary(7), storeAs = "looked"),
    SelectFromCollectionEffect(from = "looked", selection = ChooseExactly(2), storeSelected = "kept"),
    MoveCollectionEffect(from = "kept", destination = ToZone(Zone.HAND)),
    MoveCollectionEffect(from = "remainder", destination = ToZone(Zone.GRAVEYARD))
))
```

Higher-level helpers in `EffectPatterns` provide common recipes:

```kotlin
EffectPatterns.scry(2)          // Scry 2
EffectPatterns.surveil(2)       // Surveil 2 (like scry but to graveyard)
EffectPatterns.mill(3)          // Mill 3
EffectPatterns.searchLibrary(filter)  // Tutor effect
EffectPatterns.lookAtTopAndKeep(count = 7, keepCount = 2)  // Ancestral Memories
```

**Why not one effect class per card?**

- **Combinatorial reuse.** Hundreds of Magic cards manipulate libraries in slightly different ways. The
  pipeline model lets you express virtually all of them by composing the same three primitives with
  different parameters.
- **Zero new code for new cards.** Adding a card that says "Look at the top 5, put 2 on bottom, rest
  on top" requires zero new effect executors — just a new pipeline composition in `EffectPatterns`.
- **Consistent player UX.** All library manipulation flows through the same `SelectFromCollectionEffect`
  executor, which means the UI for choosing cards is consistent across all cards that use this pattern.
- **Testability.** Each primitive is unit-testable in isolation. The pipeline as a whole is tested via
  integration tests.

### 1.6 DSL as Abstraction Layer

**Principle:** A Kotlin type-safe builder DSL sits between card authors and the underlying AST.

Card definitions are written using a DSL (`card { ... }`, `spell { ... }`, `triggeredAbility { ... }`)
rather than constructing raw data classes. The DSL provides:

```kotlin
val Blaze = card("Blaze") {
    manaCost = "{X}{R}"
    typeLine = "Sorcery"
    spell {
        val t = target("target", AnyTarget())
        effect = DealDamageEffect(DynamicAmount.XValue, t)
    }
}
```

**Why a DSL instead of raw constructors?**

- **Refactoring safety.** The DSL and `Effects` facade act as an API contract. The underlying data
  structures can be refactored (fields renamed, types reorganized) without rewriting card definitions —
  only the DSL mapping layer needs updating.
- **Developer experience.** Writing raw AST structures is verbose and error-prone. The DSL provides
  IDE autocomplete, type checking, and a natural reading order that mirrors the card's oracle text.
- **Facade pattern.** The `Effects` object exposes factory methods (`Effects.DealDamage()`,
  `Effects.Destroy()`, `Effects.DrawCards()`) that hide the concrete data class constructors. Card
  authors never need to know which package an effect lives in.

---

## 2. Rules Engine: The Functional Core

The `rules-engine` module is a pure Kotlin library with zero framework dependencies (no Spring, no I/O).
It models Magic: The Gathering's rules as a deterministic state machine operating on immutable data.

### 2.1 Pure Functional State Transitions

**Principle:** The engine is a pure function: `(GameState, GameAction) -> ExecutionResult`.

Every operation in the engine takes an immutable `GameState` and a `GameAction`, and returns a new
`GameState` plus a list of `GameEvent`s. Nothing is mutated. The `ActionProcessor` is the entry point:

```kotlin
class ActionProcessor {
    fun process(state: GameState, action: GameAction): ExecutionResult
}
```

`GameState` is a Kotlin `data class` containing the complete game state: all entities and their
components, zone contents, stack, turn information, pending decisions, floating effects, and the
continuation stack:

```kotlin
data class GameState(
    val entities: Map<EntityId, ComponentContainer> = emptyMap(),
    val zones: Map<ZoneKey, List<EntityId>> = emptyMap(),
    val stack: List<EntityId> = emptyList(),
    val activePlayerId: EntityId? = null,
    val phase: Phase = Phase.BEGINNING,
    val step: Step = Step.UNTAP,
    val floatingEffects: List<ActiveFloatingEffect> = emptyList(),
    val continuationStack: List<ContinuationFrame> = emptyList(),
    val pendingDecision: PendingDecision? = null,
    // ... more fields
)
```

State updates return new instances:

```kotlin
fun withEntity(id: EntityId, container: ComponentContainer): GameState =
    copy(entities = entities + (id to container))
```

**Why immutability?**

- **No corruption during resolution.** Magic spells can trigger abilities that trigger more abilities,
  all within a single resolution step. Mutable state would risk mid-resolution corruption where a
  triggered ability sees a half-updated game state.
- **Trivial networking.** The server can send the raw `GameState` (or a diff) to clients. There's no
  hidden mutable state that could desynchronize.
- **Time travel.** Undo/redo is trivial — just keep an array of previous states. AI simulation via
  Monte Carlo Tree Search works by branching from any state without cloning.
- **Debugging.** Any bug can be reproduced by replaying the action sequence against the initial state.
  No hidden mutation means deterministic replay.

### 2.2 Entity-Component-System (ECS)

**Principle:** Every game object is an `EntityId` with behavioral traits attached via components.

Instead of an OOP class hierarchy (`class Creature : Permanent : Card`), every card, token, player,
spell, and ability is just an `EntityId` (a `value class` wrapping a UUID string). An entity's behavior is determined entirely
by which components are attached to it:

```kotlin
data class ComponentContainer(
    internal val components: Map<String, Component> = emptyMap()
) {
    inline fun <reified T : Component> get(): T?
    inline fun <reified T : Component> has(): Boolean
    inline fun <reified T : Component> with(component: T): ComponentContainer  // returns new
    inline fun <reified T : Component> without(): ComponentContainer           // returns new
}
```

Components are pure data bags — no methods, no logic:

```kotlin
data object TappedComponent : Component       // binary flag: tapped or not
data class ControllerComponent(val playerId: EntityId) : Component
data class LifeTotalComponent(val life: Int) : Component
data class CardComponent(
    val name: String,
    val typeLine: TypeLine,
    val baseStats: BaseStats?,
    val baseKeywords: Set<Keyword>,
    // ...
) : Component
```

**Why ECS instead of class inheritance?**

- **Shape-shifting.** Magic cards constantly alter their fundamental nature. A Land can become a Creature
  (via animation effects), lose all abilities, gain Flying, then turn face-down. An OOP hierarchy
  like `class Land : Permanent` would collapse when a Land needs creature stats. ECS handles this by
  simply adding `PowerToughnessComponent` — no reclassification needed.
- **Clean zone transitions.** When a creature dies (moves from battlefield to graveyard), it should
  lose all battlefield-specific state (tapped, summoning sickness, damage, attachments). ECS makes
  this trivial: `stripBattlefieldComponents()` removes the relevant components. An OOP object would
  need explicit reset logic for each field.
- **Uniform entity handling.** Players, cards, tokens, emblems, and abilities are all entities. The
  engine doesn't need separate systems for "player operations" vs. "card operations" — it just queries
  and modifies components.
- **Immutability.** `ComponentContainer` operations return new instances. This composes cleanly with
  the immutable `GameState` — modifying a component means creating a new container, which means
  creating a new entity map, which means creating a new `GameState`.

### 2.3 Rule 613: Base State vs. Projected State

**Principle:** The engine explicitly separates stored state from derived state.

Magic's Comprehensive Rules require that continuous effects (Giant Growth's +3/+3, Humility removing
all abilities, Blood Moon turning lands into Mountains) be applied in a specific layer order without
permanently modifying the underlying card. The engine solves this with two distinct state views:

- **Base State** (`GameState.entities`): The physical card data — text printed on the card, counters
  placed on it, which zone it's in. Never modified by continuous effects.
- **Projected State** (`GameState.projectedState`): The game-visible state after applying all continuous
  effects in Rule 613 layer order. Calculated lazily and cached per immutable `GameState` instance.

The `StateProjector` applies effects in order:

```
Layer 1: Copy effects
Layer 2: Control-changing effects
Layer 3: Text-changing effects
Layer 4: Type-changing effects
Layer 5: Color-changing effects
Layer 6: Ability-adding/removing effects
Layer 7: Power/toughness (sublayers a-e)
```

Within each layer, effects are sorted by **dependency** using trial application (Rule 613.8): the engine
checks whether applying effect A changes the outcome of effect B, establishing ordering constraints
before falling back to timestamp ordering.

**Why not just mutate the card's stats?**

- **Reversibility.** When Giant Growth's duration expires at end of turn, the engine doesn't need to
  "undo" +3/+3. It simply removes the floating effect. The next projection recalculates from base
  state and the creature returns to its printed stats.
- **Layer interactions.** If Humility (removes all abilities, sets P/T to 1/1) and a +1/+1 counter
  are both on a creature, the answer depends on layer ordering and timestamps. Mutating stats would
  make this impossible to resolve correctly. Recalculating from base state ensures accuracy.
- **Inspection.** The engine (and UI) can show both "what the card says" and "what the game sees" —
  useful for debugging and player understanding.

### 2.4 Reentrant Continuations

**Principle:** When the engine needs player input, it pauses and saves a serializable continuation.

Many Magic cards require player decisions mid-resolution — "search your library for a card" requires
the player to browse and choose. The engine cannot block a thread waiting for network input. Instead,
it pauses by:

1. Setting `GameState.pendingDecision` to describe what input is needed
2. Pushing a `ContinuationFrame` onto `GameState.continuationStack` that describes how to resume

```kotlin
sealed interface ContinuationFrame {
    val decisionId: String
}

data class EffectContinuation(
    override val decisionId: String,
    val remainingEffects: List<Effect>,
    val sourceId: EntityId?,
    val controllerId: EntityId,
    val storedCollections: Map<String, List<EntityId>>,
    // ... all context needed to resume
) : ContinuationFrame
```

When the player submits their decision, `ContinuationHandler.resume()` pops the frame, restores
context, and continues executing the remaining effects.

**Why serializable continuations instead of coroutines or blocked threads?**

- **Server scalability.** No threads are blocked waiting for network I/O. The server can serialize
  the paused state, spin down the game session, and restore it when the player responds — even on a
  different server instance.
- **Nested resolution.** Magic allows abilities to trigger during spell resolution (e.g., paying a
  cost triggers an ability that also requires decisions). The continuation *stack* naturally handles
  this nesting — each new decision pushes a frame, and resolution proceeds LIFO.
- **Persistence.** Because `ContinuationFrame` is `@Serializable`, the entire paused game state can
  be saved to disk and resumed later — enabling server restarts without losing game state.
- **Deterministic replay.** The continuation stack is part of `GameState`. A replay log of actions
  deterministically reproduces the exact sequence of decisions and resumptions.

### 2.5 Explicit Event Emission

**Principle:** Every state mutation emits an explicit, typed event.

When the engine deals damage, draws a card, or changes zones, it produces a `GameEvent`:

```kotlin
@Serializable
sealed interface GameEvent

data class ZoneChangeEvent(
    val entityId: EntityId,
    val entityName: String,
    val fromZone: Zone?,
    val toZone: Zone,
    val ownerId: EntityId
) : GameEvent

data class DamageDealtEvent(
    val sourceId: EntityId?,
    val targetId: EntityId,
    val amount: Int,
    val isCombatDamage: Boolean,
    val sourceName: String? = null,
) : GameEvent

data class LifeChangedEvent(
    val playerId: EntityId,
    val oldLife: Int,
    val newLife: Int,
    val reason: LifeChangeReason
) : GameEvent

// ... many more
```

The `TriggerDetector` observes these events to determine which triggered abilities fire:

```kotlin
class TriggerDetector {
    fun detectTriggers(state: GameState, events: List<GameEvent>): List<PendingTrigger>
}
```

**Why explicit events instead of polling or observer patterns?**

- **Decoupling.** The `CombatManager` dealing damage doesn't need to know about "Enrage" abilities.
  It emits `DamageDealtEvent`; the `TriggerDetector` independently determines that this fires an
  Enrage trigger. New triggered abilities never require changes to the code that produces events.
- **Simultaneous damage.** Magic requires combat damage to happen simultaneously, and creatures that
  die in the same batch can still see each other's death triggers (Rule 603.10 "look back in time").
  Batched event emission enables this — events are collected during combat damage, then triggers are
  detected from the complete batch.
- **Client animation.** Events are forwarded to the client for animation (damage numbers, card draws,
  zone transitions). Explicit typed events give the client enough information to animate without
  needing to diff entire game states.
- **Game log.** The event stream is a complete, structured game log that can be replayed, analyzed,
  or displayed to spectators.

### 2.6 Strategy-Based Registries

**Principle:** Action handling and effect execution are fully decoupled from the core engine loop.

The `ActionProcessor` doesn't contain a giant `when` statement over all possible actions. Instead, it
delegates to an `ActionHandlerRegistry` where each action type has a registered handler:

```kotlin
private val registry = ActionHandlerRegistry().apply {
    registerModule(PriorityModule(context))
    registerModule(LandModule(context))
    registerModule(CombatModule(context))
    registerModule(SpellModule(context))
    registerModule(DecisionModule(context))
    // ...
}
```

Similarly, `EffectExecutorRegistry` maps each `Effect` type to its executor. Adding a new mechanic
means creating a new `EffectExecutor` and registering it — no changes to `ActionProcessor`.

**Why registries instead of a monolithic processor?**

- **Open/closed principle.** New action types and effect types can be added without modifying existing
  code. The core engine loop is closed for modification but open for extension.
- **Testability.** Individual handlers and executors can be tested in isolation. Mock executors can be
  injected during tests to verify pipeline behavior without loading the entire card database.
- **Modularity.** Related handlers are grouped into modules (`CombatModule`, `SpellModule`). Each
  module is self-contained and can be understood independently.

### 2.7 Replacement Effects

**Principle:** Replacement effects intercept game events *before* they happen, modifying them without
using the stack.

Unlike triggered abilities (which fire after an event and go on the stack), replacement effects are
interceptors that modify an action as it occurs. Doubling Season doubles the number of tokens created;
Rest in Peace redirects cards going to the graveyard to exile instead. These are modeled in the SDK
as a `ReplacementEffect` sealed interface with a compositional `appliesTo` filter:

```kotlin
sealed interface ReplacementEffect {
    val description: String
    val appliesTo: GameEvent  // What event this intercepts
}

// Doubling Season: double token creation
data class DoubleTokenCreation(
    override val appliesTo: GameEvent = GameEvent.TokenCreationEvent()
) : ReplacementEffect

// Hardened Scales: add extra +1/+1 counters
data class ModifyCounterPlacement(
    val modifier: Int,
    override val appliesTo: GameEvent = GameEvent.CounterPlacementEvent(
        counterType = CounterTypeFilter.PlusOnePlusOne,
        recipient = RecipientFilter.CreatureYouControl
    )
) : ReplacementEffect

// Rest in Peace: redirect graveyard to exile
data class RedirectZoneChange(
    val newDestination: Zone,
    override val appliesTo: GameEvent = GameEvent.ZoneChangeEvent(to = Zone.GRAVEYARD)
) : ReplacementEffect
```

The engine checks for applicable replacement effects during zone changes, damage, token creation,
and counter placement. Because replacement effects are data (not callbacks), the engine can inspect
which replacements apply, handle ordering when multiple replacement effects compete (the affected
player chooses), and serialize the state even when a replacement is pending.

**Why model replacement effects as data interceptors?**

- **No stack interaction.** Replacement effects modify events in-place — they don't use the stack and
  can't be responded to. Modeling them as interceptors that the engine checks at specific points
  (zone changes, damage, counters) mirrors the rules naturally.
- **Composability.** The `appliesTo` field uses the same `GameEvent` filter system as trigger
  conditions. A replacement effect that applies to "damage dealt to creatures you control" reuses
  the same predicate composition as a trigger that fires on the same event.

### 2.8 State-Based Actions

**Principle:** The engine continuously enforces game invariants via automatic state-based actions.

Magic's Rule 704 defines state-based actions (SBAs) — conditions that the game checks and enforces
automatically whenever a player would receive priority. These aren't player actions; they're
invariants the game maintains. The `StateBasedActionChecker` implements this as a loop that runs
until no more SBAs apply:

```kotlin
class StateBasedActionChecker {
    fun checkAndApply(state: GameState): ExecutionResult {
        var currentState = state
        val allEvents = mutableListOf<GameEvent>()

        var actionsApplied: Boolean
        do {
            val result = checkOnce(currentState)
            actionsApplied = result.events.isNotEmpty()
            currentState = result.newState
            allEvents.addAll(result.events)
        } while (actionsApplied)

        return ExecutionResult.success(currentState, allEvents)
    }
}
```

Each pass checks the following rules (among others):

- **704.5a** — Player with 0 or less life loses
- **704.5b** — Player with 10+ poison counters loses
- **704.5f** — Creature with 0 or less toughness goes to graveyard
- **704.5g** — Creature with lethal damage goes to graveyard
- **704.5i** — Planeswalker with 0 loyalty goes to graveyard
- **704.5j** — Legend rule (two legendary permanents with same name)
- **704.5m** — +1/+1 and -1/-1 counters on the same permanent annihilate
- **704.5n** — Unattached auras go to graveyard
- **704.5s** — Tokens in non-battlefield zones cease to exist

**Why a repeating loop?**

SBAs can trigger more SBAs. For example, destroying an aura (because the enchanted creature died
from lethal damage) might leave a different creature with 0 toughness if the aura was granting
toughness. The loop continues until the game reaches a stable state. Events emitted during SBA
processing feed into trigger detection afterward, ensuring death triggers and similar abilities
fire correctly.

---

## 3. Game Server: The Orchestration Layer

The `game-server` module is a Spring Boot application that wraps the rules engine with networking,
session management, and state transformation. It contains **zero game logic** — that all lives in
the engine.

### 3.1 Thin Server, Zero Game Logic

**Principle:** The server is a thin orchestration layer, not a game engine.

The `GameSession` class wraps the engine's `ActionProcessor` with thread safety and session management:

```kotlin
class GameSession {
    private val actionProcessor = ActionProcessor(cardRegistry)
    private var gameState: GameState? = null

    fun processAction(playerId: EntityId, action: GameAction) {
        synchronized(stateLock) {
            val result = actionProcessor.process(gameState!!, action)
            gameState = result.state
            broadcastStateUpdate()
        }
    }
}
```

The server's responsibilities are strictly limited to:
- **WebSocket routing:** Receiving client messages and dispatching to the right session
- **Session management:** Player authentication, reconnection, spectating
- **State transformation:** Converting engine state to client-safe DTOs
- **Legal action calculation:** Computing what moves are available (but not *executing* them)

**Why keep the server thin?**

- **Single source of truth.** Game logic lives in exactly one place (the engine). There's no risk of
  the server and engine disagreeing about rules.
- **Testability.** The engine can be tested exhaustively without spinning up a server. Scenario tests
  run in milliseconds against the pure `ActionProcessor`.
- **Portability.** The engine is a pure Kotlin library. It could be embedded in an Android app, a CLI
  tool, or an AI simulation framework without any server infrastructure.

### 3.2 State Masking (Fog of War)

**Principle:** Each player sees a filtered view of the game state.

In Magic, players cannot see each other's hands or libraries. The `ClientStateTransformer` produces a
per-player view of the state that hides private information:

- **Libraries:** Always hidden — only the count is visible
- **Opponent's hand:** Only the count is visible, not the card identities
- **Face-down cards:** Show as generic "Card back" to all players (even the controller cannot see
  the identity in the masked view — only when they choose to interact)
- **Revealed cards:** `RevealedToComponent` overrides hiding for cards that have been explicitly revealed

**Why server-side masking instead of client-side filtering?**

- **Security.** If the full game state were sent to clients, a modified client could read the opponent's
  hand and library. Server-side masking ensures the data never leaves the server.
- **Simplicity.** The client doesn't need to know masking rules. It renders exactly what it receives.

### 3.3 Anti-Corruption Layer (DTO Transformation)

**Principle:** The client never sees engine internals.

The `ClientStateTransformer` converts the engine's `GameState` into `ClientGameState`, a completely
separate data model designed for the client's needs:

```
GameState (engine internal)
    → ClientStateTransformer
        → ClientGameState (client DTO)
            → JSON over WebSocket
```

The engine uses `EntityId`, `ComponentContainer`, `ZoneKey`, and other internal types. The client
receives `ClientCard`, `ClientZone`, `ClientPlayer` — flat, self-contained objects with all the
information needed for rendering.

**Why a separate DTO layer?**

- **Decoupling.** Engine refactors (renaming components, restructuring state) don't break the client.
  The transformer absorbs the change.
- **Optimization.** The client DTO includes only what's needed for rendering. Projected stats are
  pre-computed, mana costs are formatted, and irrelevant internal data is stripped. After the first
  full state update, a `StateDiffCalculator` sends only the changed cards, zones, and scalars via
  `StateDeltaUpdate` messages — reducing bandwidth for incremental updates.
- **Versioning.** The DTO contract can be versioned independently of the engine. This enables
  backwards-compatible client updates.

### 3.4 Server-Authoritative Legal Actions

**Principle:** The server computes and sends every legal action with each state update.

Every `StateUpdate` message includes a `legalActions` list — the complete set of moves the priority
player can make:

```kotlin
// ServerMessage.StateUpdate — sent to each player after every action
data class StateUpdate(
    val state: ClientGameState,
    val events: List<ClientEvent>,
    val legalActions: List<LegalActionInfo>,
    val pendingDecision: PendingDecision? = null,
    val nextStopPoint: String? = null,
    val opponentDecisionStatus: OpponentDecisionStatus? = null,
    // ... more fields
) : ServerMessage
```

The `LegalActionsCalculator` evaluates the current game state against the rules to determine what's
legal: which spells can be cast (considering mana, timing, targets), which abilities can be activated,
whether lands can be played, and what combat actions are available.

**Why compute legal actions server-side?**

- **Correctness.** Determining legal actions requires full rules knowledge — mana availability,
  timing restrictions, target legality, spell interaction restrictions. This logic already exists
  in the engine. Duplicating it in the client would be error-prone and a maintenance burden.
- **Security.** The server validates every action before executing it. Even a modified client
  cannot perform illegal moves.
- **Client simplicity.** The client simply checks "is there a legal action involving this card?"
  to decide whether a card is clickable. Zero game rules knowledge required.

### 3.5 Auto-Pass Priority System

**Principle:** The server automatically passes priority when there's nothing meaningful to do.

Magic's priority system requires each player to explicitly pass priority before anything resolves or
phases advance. Without auto-pass, players would click "pass" dozens of times per turn. The
`AutoPassManager` implements Arena-style intelligent priority passing with four rules:

1. **Meaningful Action Filter:** Mana abilities don't count as responses. Spells with no valid
   targets are invisible. Counterspells only matter when the stack is non-empty.
2. **Opponent's Turn Compression:** Auto-pass through upkeep and draw, but stop during combat
   (crucial window for removal) and end step (control player's golden window).
3. **My Turn Optimization:** Auto-pass through upkeep and draw, but stop during combat (for
   tricks like Giant Growth).
4. **Stack Response:** Auto-pass if your own spell is on top; stop if opponent's non-permanent
   spell is on top (counterspell opportunity).

Players can override this with per-step stops (e.g., "always stop in opponent's upkeep") or full
control mode that disables all auto-passing.

**Why server-side auto-pass?**

- **Consistency.** Auto-pass logic depends on the legal actions list and game state — both server-side.
  Having the server handle it ensures consistent behavior regardless of client implementation.
- **No information leakage.** If the client decided when to auto-pass, the timing of responses
  could leak information about hidden cards (e.g., slow response = has instant in hand).

### 3.6 Tournament System

**Principle:** The server manages multi-game tournament lifecycles as a state machine.

Beyond individual games, the server supports full tournament play — Sealed, Draft, Winston Draft,
and Grid Draft formats — managed by `TournamentLobby` and `TournamentManager`. The lobby follows
a state machine:

```
WAITING_FOR_PLAYERS → DRAFTING → DECK_BUILDING → TOURNAMENT_ACTIVE → TOURNAMENT_COMPLETE
                        (draft)
WAITING_FOR_PLAYERS → DECK_BUILDING → TOURNAMENT_ACTIVE → TOURNAMENT_COMPLETE
                        (sealed)
```

`TournamentManager` generates a full round-robin schedule upfront using the circle method (N-1
rounds for N players), then matches start dynamically as players become ready — no waiting for
the entire round to finish. This "eager match starting" pattern means:

1. A player sends `ReadyForNextRound`
2. The server checks if their next opponent is also ready
3. If both are ready, the match starts immediately via a new `GameSession`
4. Other players continue waiting or playing their own matches

BYE handling (odd player counts), reconnection across matches, and spectating between rounds are
all managed at this layer. The tournament system reuses the same `GameSession` and `ActionProcessor`
for each match — it's purely orchestration, with no game logic of its own.

---

## 4. Web Client: The Dumb Terminal

The `web-client` is a React application that follows the **dumb terminal** pattern: it renders what
the server tells it to render and captures user intent without any game logic.

### 4.1 No Game Logic in the Client

**Principle:** The client contains zero Magic: The Gathering rules.

The client does not know:
- How mana works
- What phases and steps exist in a turn
- How combat damage is calculated
- Whether a spell can legally target something
- What happens when a card resolves

It knows only how to:
- Render cards in zones (hand, battlefield, graveyard, stack)
- Show interactive indicators on cards that have legal actions
- Capture clicks and forward them as structured messages

**Why a dumb client?**

- **No divergence.** If the client computed game state independently, it could diverge from the server
  during edge cases. A dumb client always shows exactly what the server says.
- **Maintenance.** Magic rules are complex and change frequently. Maintaining a rules implementation
  in TypeScript alongside the Kotlin engine would be an enormous burden.
- **Platform portability.** A new client (mobile, CLI, accessibility) only needs to render and
  capture input — no rules reimplementation required.

### 4.2 Server-Driven Interactivity

**Principle:** The legal actions list drives all client interactivity.

Cards glow, buttons appear, and targeting becomes available only when the server says so. The
`useLegalActions` hook categorizes server-provided actions:

```typescript
export function useCategorizedActions(): CategorizedActions {
    const legalActions = useGameStore((state) => state.legalActions)
    return useMemo(() => {
        const result: CategorizedActions = {
            landPlays: [],
            spellCasts: [],
            activatedAbilities: [],
            passPriority: null,
            declareAttackers: null,
            declareBlockers: null,
            other: [],
        }
        for (const action of legalActions) {
            switch (action.action.type) {
                case 'PlayLand': result.landPlays.push(action); break
                case 'CastSpell': result.spellCasts.push(action); break
                // ...
            }
        }
        return result
    }, [legalActions])
}
```

When a player clicks a card, `useInteraction` checks whether any legal action references that card.
If there's exactly one action, it executes immediately. If there are multiple (e.g., a creature with
an activated ability can also attack), it shows a menu.

**Why server-driven instead of client-computed?**

- **Zero duplication.** Legal action computation is complex and already exists server-side. The client
  gets it for free.
- **Impossible to cheat.** The client physically cannot present illegal options because it only shows
  what the server authorizes.
- **Adaptive UI.** When the server adds a new action type (e.g., a new keyword mechanic), the client
  can display it immediately as a generic action button without any client-side update.

### 4.3 Zustand Slice Architecture

**Principle:** Client state is organized into domain-specific slices.

The Zustand store is split into five independent slices:

| Slice | Responsibility |
|-------|----------------|
| `connectionSlice` | WebSocket connection, authentication, reconnection |
| `gameplaySlice` | Game state, legal actions, events, decisions |
| `lobbySlice` | Tournament lobbies, matchmaking, spectating |
| `draftSlice` | Sealed/draft deck building |
| `uiSlice` | Client-only state: targeting arrows, combat UI, animations |

**Why slices?**

- **Separation of concerns.** UI state (which card is being dragged) is completely separate from
  game state (what cards are on the battlefield). Neither can accidentally corrupt the other.
- **Granular subscriptions.** Components subscribe only to the slice they need. A targeting arrow
  component doesn't re-render when the game log updates.
- **Testability.** Each slice can be tested independently with mock data.

### 4.4 Intent Capture, Not Computation

**Principle:** The client captures player intent and lets the server figure out the mechanics.

When a player wants to attack with a creature, the client doesn't calculate whether the attack is
legal, whether the creature has summoning sickness, or whether there's a "can't attack" effect. It:

1. Shows the creature as a valid attacker (because the server's legal actions say so)
2. Lets the player select it
3. Sends `DeclareAttackers { attackers: [creatureId] }` to the server
4. The server validates, executes, and sends back the updated state

Similarly, targeting is just "the player clicked on entity X." The server determines whether that's
a legal target, resolves the spell, and sends the result.

**Why intent capture?**

- **Simplicity.** The client interaction code is trivial — check if legal, let user pick, send message.
  No edge cases, no rules knowledge.
- **Robustness.** The server always validates. Even if the client sends an illegal action (due to a
  race condition or bug), the server rejects it gracefully.

---

## 5. Testing: Multi-Layered Verification

The testing strategy mirrors the architecture: each layer has its own testing approach, and higher
layers build on the guarantees of lower ones.

### 5.1 Unit and Integration Tests

**Engine-level tests** use Kotest's FunSpec with a `GameTestDriver` that wraps the `ActionProcessor`:

```kotlin
class CreatureStatsTest : FunSpec({
    test("Giant Growth gives +3/+3 until end of turn") {
        val driver = GameTestDriver()
        // ... setup, cast Giant Growth, assert stats
    }
})
```

These tests run in milliseconds against the pure functional engine — no server, no network, no UI.
They verify individual mechanics (damage, combat, triggered abilities, state-based actions) in
isolation.

### 5.2 Scenario Tests

**Scenario tests** verify complete card interactions using `ScenarioTestBase`, which provides a
builder pattern for setting up specific board states and playing through sequences:

```kotlin
class AncestralMemoriesScenarioTest : ScenarioTestBase() {
    init {
        test("look at top 7, keep 2") {
            // Set up specific library contents
            // Cast Ancestral Memories
            // Verify selection prompt
            // Choose 2 cards
            // Verify hand and graveyard contents
        }
    }
}
```

Scenario tests are the primary verification for card correctness. Each card typically has one or more
scenario tests covering its main functionality and edge cases.

### 5.3 End-to-End Tests with Dev Scenario API

**E2E tests** run against the full stack (server + client) in a real browser using Playwright. The
`DevScenarioController` provides a REST API that injects a specific board state directly into a
`GameSession`, bypassing the normal game initialization:

```typescript
test('Lightning Bolt deals 3 damage', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
        player1: {
            hand: ['Lightning Bolt'],
            battlefield: [{ name: 'Mountain', tapped: false }],
            library: ['Mountain'],
        },
        player2: {
            battlefield: [{ name: 'Glory Seeker' }],
            library: ['Mountain'],
        },
        phase: 'PRECOMBAT_MAIN',
        activePlayer: 1,
    })

    await player1.gamePage.clickCard('Lightning Bolt')
    await player1.gamePage.selectAction('Cast')
    await player1.gamePage.selectTarget('Glory Seeker')
    await player1.gamePage.confirmTargets()
    await player1.gamePage.expectNotOnBattlefield('Glory Seeker')
})
```

**Why a Dev Scenario API?**

- **Deterministic setup.** E2E tests need precise board states. Drawing random cards from shuffled
  decks would make tests flaky. The scenario API bypasses randomness entirely.
- **Fast iteration.** Tests start with the exact state they need — no multi-turn setup to reach a
  specific game position.
- **Full-stack coverage.** These tests exercise the entire pipeline: WebSocket handling, state
  transformation, masking, client rendering, user interaction, and action processing.
- **Separated concerns.** The `GamePage` page object encapsulates all UI interaction, so tests read
  like natural-language descriptions of game actions.

---

## Summary

The Argentum Engine's architecture is organized around a core insight: **Magic: The Gathering's rules
are too complex for ad-hoc engineering.** Each architectural decision addresses a specific source of
complexity:

| Complexity Source | Architectural Response |
|---|---|
| Cards change their fundamental nature | ECS over OOP inheritance |
| Continuous effects layer and interact | Base state + projected state (Rule 613) |
| Spells require mid-resolution decisions | Serializable continuations |
| Events modify other events before they happen | Replacement effects as data interceptors |
| Game invariants must be continuously enforced | State-based action checker (Rule 704 loop) |
| 20,000+ unique cards | Data-driven definitions + DSL |
| Library manipulation has infinite variants | Atomic effect pipelines (Gather/Select/Move) |
| Dynamic values that change constantly | AST-based DynamicAmount |
| Multiplayer with hidden information | Server-authoritative state + masking |
| Client-server state divergence | Dumb terminal pattern |
| Complex legal action computation | Server-driven interactivity |
| Multi-game tournaments with drafting | State machine lobby + eager match starting |
| Testing edge cases across the stack | Dev Scenario API + multi-layer test pyramid |

The result is a system where each layer has a clear, minimal responsibility — the SDK describes,
the engine executes, the server orchestrates, and the client renders.