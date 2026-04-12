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
   - [2.9 Turn Structure and Priority](#29-turn-structure-and-priority)
   - [2.10 Mana and Cost Payment](#210-mana-and-cost-payment)
   - [2.11 Copy Effects](#211-copy-effects)
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
   - [5.1 Unit and Integration Tests (Engine Layer)](#51-unit-and-integration-tests-engine-layer)
   - [5.2 Scenario Tests (Card Interaction Layer)](#52-scenario-tests-card-interaction-layer)
   - [5.3 End-to-End Tests with Dev Scenario API (Full Stack Layer)](#53-end-to-end-tests-with-dev-scenario-api-full-stack-layer)

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

**The target lifecycle.** To see why symbolic references matter, consider what happens when a player
casts "Doom Blade — destroy target nonblack creature":

```
1. CAST TIME: Player selects a creature (e.g., entity "bear-123")
   → CastSpellHandler validates target against TargetRequirement
   → ChosenTarget.Permanent("bear-123") stored in TargetsComponent on the spell entity
   → Spell goes on the stack

2. ON THE STACK: Opponent gets priority and can respond
   → The spell's effect still says ContextTarget(0), not "bear-123"
   → The chosen target is stored separately on the spell entity, not in the effect

3. RESOLUTION: Both players pass, spell resolves
   → StackResolver retrieves TargetsComponent from the spell entity
   → Re-validates each target against the original requirements (Rule 608.2b)
   → If the creature gained hexproof or left the battlefield: spell fizzles
   → If still legal: builds EffectContext(targets = [ChosenTarget.Permanent("bear-123")])

4. EFFECT EXECUTION: Engine runs the DestroyEffect
   → Effect references EffectTarget.ContextTarget(0)
   → resolveTarget() looks up context.targets[0] → "bear-123"
   → Creature is destroyed
```

The key insight is that `ContextTarget(0)` is never resolved until step 4. The effect definition
doesn't know or care which entity was chosen — it just says "the first target." This separation
enables the engine to recheck legality at step 3 without any special-case code.

**Why this matters:**

- **Target legality rechecking.** Re-validation at resolution is mandatory per Rule 608.2b. Because
  the chosen target (`ChosenTarget`) is stored separately from the effect reference
  (`ContextTarget`), the engine can validate one against the other naturally — the reference is the
  "slot," the chosen target is the "binding."
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

Every operation in the engine takes an immutable `GameState` and a `GameAction`, and returns an
`ExecutionResult` containing a new `GameState` plus a list of `GameEvent`s. Nothing is mutated.
The `ActionProcessor` is the entry point:

```kotlin
class ActionProcessor {
    fun process(state: GameState, action: GameAction): ProcessedAction
}

data class ProcessedAction(
    val result: ExecutionResult,
    val undoPolicy: UndoCheckpointAction = UndoCheckpointAction.CLEAR
)
```

`ProcessedAction` pairs the core result with an undo checkpoint policy — the engine computes
the policy based on game rules, and the server follows it mechanically. `ExecutionResult` itself
captures the three possible outcomes of any action:

```kotlin
data class ExecutionResult(
    val state: GameState,
    val events: List<GameEvent> = emptyList(),
    val error: String? = null,
    val pendingDecision: PendingDecision? = null
)
```

The `PausedForDecision` case is central to how the engine handles player input mid-resolution — when a
spell requires a choice (e.g., "search your library for a card"), the engine doesn't block. It returns a
paused result with a `PendingDecision` describing what input is needed and a `ContinuationFrame` on the
state's continuation stack describing how to resume (see [Section 2.4](#24-reentrant-continuations)).

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

**What does a creature look like?** A vanilla 2/2 creature (Grizzly Bears) on the battlefield is
just an `EntityId` with these components attached:

```
EntityId("bears-abc123") → ComponentContainer {
    CardComponent        → name="Grizzly Bears", manaCost={1}{G}, typeLine=Creature—Bear,
                           baseStats=2/2, baseKeywords=[], colors=[GREEN]
    OwnerComponent       → playerId="player-1"       (immutable — who owns the card)
    ControllerComponent  → playerId="player-1"       (mutable — who controls it now)
    SummoningSicknessComponent                        (tag — removed on controller's next turn)
}
```

That's it — four components. When the creature takes damage, a `DamageComponent(3)` is added. When
it taps to attack, `TappedComponent` and `AttackingComponent` are added. When it's enchanted,
the aura entity gets an `AttachedToComponent(targetId="bears-abc123")`. Each state change adds or
removes a component, building up the entity's identity dynamically.

When the creature dies (moves to graveyard), `stripBattlefieldComponents()` removes all transient
state — `ControllerComponent`, `TappedComponent`, `SummoningSicknessComponent`, `DamageComponent`,
combat state, attachments — leaving only the immutable identity (`CardComponent`, `OwnerComponent`).

**Why ECS instead of class inheritance?**

- **Shape-shifting.** Magic cards constantly alter their fundamental nature. A Land can become a Creature
  (via animation effects), lose all abilities, gain Flying, then turn face-down. An OOP hierarchy
  like `class Land : Permanent` would collapse when a Land needs creature stats. ECS handles this by
  simply adding `PowerToughnessComponent` — no reclassification needed.
- **Clean zone transitions.** When a creature dies, `stripBattlefieldComponents()` removes the
  relevant components in one call. An OOP object would need explicit reset logic for each field.
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

Similarly, `EffectExecutorRegistry` maps each `Effect` type to its executor, and
`LegalActionEnumerator` delegates to per-category `ActionEnumerator` implementations to discover
legal actions. Adding a new mechanic means creating a new `EffectExecutor` and `ActionEnumerator`
and registering them — no changes to `ActionProcessor` or `LegalActionEnumerator`.

**Why registries instead of a monolithic processor?**

- **Open/closed principle.** New action types and effect types can be added without modifying existing
  code. The core engine loop is closed for modification but open for extension.
- **Testability.** Individual handlers and executors can be tested in isolation. Mock executors can be
  injected during tests to verify pipeline behavior without loading the entire card database.
- **Modularity.** Related handlers are grouped into modules (`CombatModule`, `SpellModule`). Each
  module is self-contained and can be understood independently.

### 2.7 Replacement Effects

**Principle:** Replacement effects modify game actions *before* they produce events, without using
the stack.

Unlike triggered abilities (which fire *after* an event and go on the stack), replacement effects
intercept an action as it occurs and change its outcome. Doubling Season doubles the number of tokens
created; Rest in Peace redirects cards going to the graveyard to exile instead.

These are modeled in the SDK as a `ReplacementEffect` sealed interface. Each replacement declares an
`appliesTo` pattern — a `GameEvent` filter that describes *which* game actions it intercepts. Note
that `GameEvent` here is an SDK-level pattern-matching type (compositional filters over event types,
recipients, and sources), not the engine's runtime `GameEvent` instances. The SDK defines *what* to
match; the engine checks these patterns against actual game actions at execution time:

```kotlin
sealed interface ReplacementEffect {
    val description: String
    val appliesTo: GameEvent  // Pattern describing which actions to intercept
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

The engine checks for applicable replacement effects at specific interception points — zone changes,
damage assignment, token creation, counter placement, draw, life gain, and discard. Because
replacement effects are data (not callbacks), the engine can inspect which replacements apply, handle
ordering when multiple replacement effects compete (the affected player chooses per Rule 616.1), and
serialize the state even when a replacement choice is pending.

**Why model replacement effects as declarative patterns?**

- **No stack interaction.** Replacement effects modify actions in-place — they don't use the stack and
  can't be responded to. Modeling them as patterns that the engine checks at specific interception
  points mirrors the rules naturally.
- **Composability.** The `appliesTo` field uses the same `GameEvent` pattern system as trigger
  conditions. A replacement effect that applies to "damage dealt to creatures you control" reuses
  the same predicate composition as a trigger that fires on the same event — `RecipientFilter`,
  `SourceFilter`, and `DamageType` are shared between both systems.

**Ordering multiple replacement effects.** When multiple replacement effects would apply to the same
event, Rule 616.1 requires the affected player (or the controller of the affected object) to choose
which applies first. The engine handles this pragmatically rather than with a fully general Rule 616
framework:

- **Combat damage prevention** is the primary case where ordering matters. When a single damage
  prevention shield (e.g., "prevent the next 3 damage") can't cover all incoming damage from
  multiple attackers, the engine pauses with a `DistributeDecision` and a
  `DamagePreventionContinuation`. The player distributes the shield across sources, and the engine
  creates source-specific prevention effects for each allocation.
- **Non-combat replacements** (draw replacement, token doubling, counter doubling, damage
  amplification) are applied sequentially — each category has a well-defined application point in
  the engine, and multiple effects of the same type stack naturally (two doublers quadruple).
  Player choice between competing replacements of *different* types is not yet needed because the
  engine's card pool hasn't required it.

This design reflects a pragmatic choice: handle the critical case (combat damage distribution) with
full player agency via the continuation system, while leaving other replacements to deterministic
application order. The `ReplacementEffect` data model is expressive enough to support a general
Rule 616 framework if future cards require it — the `appliesTo` patterns already provide the
information needed to detect competing replacements.

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
        val allEvents = mutableListOf<GameEvent>()  // local accumulator — purely internal

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

Note the `mutableListOf` — this is local mutation within a pure function. The function takes an
immutable `GameState`, uses mutable locals to accumulate results across loop iterations, and returns
a new immutable `ExecutionResult`. No external state is modified. This is a common Kotlin idiom where
a `fold` would be less readable due to the loop-until-stable-state pattern.

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

### 2.9 Turn Structure and Priority

**Principle:** The turn is a state machine of phases and steps, with priority as the mechanism that
gates all player interaction.

Magic's turn structure is modeled as two enums — `Phase` (5 values) and `Step` (13 values) — with
the `TurnManager` driving progression:

```
Beginning Phase:    UNTAP → UPKEEP → DRAW
Precombat Main:     PRECOMBAT_MAIN
Combat Phase:       BEGIN_COMBAT → DECLARE_ATTACKERS → DECLARE_BLOCKERS
                    → FIRST_STRIKE_COMBAT_DAMAGE → COMBAT_DAMAGE → END_COMBAT
Postcombat Main:    POSTCOMBAT_MAIN
Ending Phase:       END → CLEANUP
```

Each step has a `hasPriority` property. Most steps grant priority; UNTAP and CLEANUP do not (the
engine auto-advances past them). Steps that don't grant priority execute their actions (untap all
permanents, discard to hand size) and immediately advance to the next step.

**Priority and the "both players pass" rule.** `GameState` tracks priority with two fields:

```kotlin
val priorityPlayerId: EntityId? = null           // who currently holds priority
val priorityPassedBy: Set<EntityId> = emptySet() // players who passed this round
```

When a player passes priority, the engine adds them to `priorityPassedBy` and checks
`allPlayersPassed()`. Two outcomes are possible:

1. **Stack is non-empty:** The top item resolves. After resolution, the engine runs state-based
   actions, detects triggers, and gives priority back to the active player with `priorityPassedBy`
   reset.
2. **Stack is empty:** The `TurnManager` advances to the next step. `priorityPassedBy` is cleared,
   step-specific actions execute (draw a card, deal combat damage, etc.), and priority goes to the
   active player.

If not all players have passed, priority moves to the next player in turn order. This naturally
implements Rule 117.3b — the game only advances when all players pass in succession without anyone
taking an action.

**Step-specific auto-actions.** The `TurnManager.advanceStep()` method handles each step's built-in
behavior:

- **UNTAP:** Untap all permanents, remove summoning sickness. No priority — recursively advances.
- **DRAW:** Draw a card, check draw replacement effects, then run state-based actions.
- **DECLARE_ATTACKERS / DECLARE_BLOCKERS:** Skip entirely if no valid attackers/blockers exist.
- **COMBAT_DAMAGE:** Apply all combat damage simultaneously, then run state-based actions.
- **CLEANUP:** Discard to hand size, remove damage, expire end-of-turn effects. Normally no
  priority — but if SBAs or triggers occur during cleanup, a new cleanup step begins with priority.

**Trigger detection at step boundaries.** When the stack empties and the game advances, the engine
runs three rounds of trigger detection: standard event-based triggers (from events emitted during
advancement), delayed triggers (scheduled for specific future steps, e.g., Astral Slide's "return at
end of turn"), and phase/step triggers (permanents with "at the beginning of your upkeep" abilities).
All detected triggers are processed via `TriggerProcessor`, which may pause for targeting decisions
using the continuation system.

**Why model priority as a passed-by set?**

- **Multiplayer-ready.** The `priorityPassedBy` set scales naturally to 3+ player games. The game
  advances only when all players in `turnOrder` have passed, not just two.
- **Reset semantics.** Any player action (casting a spell, activating an ability) clears the set via
  `withPriority()`, forcing all players to pass again — exactly matching Rule 117.3b.
- **No hidden state.** Priority is fully captured in the immutable `GameState`. The server can
  serialize a paused game mid-priority-round and resume it later without losing track of who has
  passed.

### 2.10 Mana and Cost Payment

**Principle:** Costs are a three-tier system — declaration (SDK), validation (solver), and execution
(handler) — with smart automatic payment.

Mana is one of Magic's most complex subsystems. The engine handles it through three cooperating
layers:

**Tier 1: Cost Declaration (SDK).** The SDK defines three separate cost hierarchies for different
contexts:

| Cost Type | Context | Examples |
|-----------|---------|----------|
| `ManaCost` | Spell casting | `{2}{B}{R}`, `{X}{G}`, `{G/U}` (hybrid), `{W/P}` (Phyrexian) |
| `AbilityCost` | Activated abilities | `Tap`, `Sacrifice(filter)`, `PayLife(3)`, `Mana(cost)`, `Composite(...)` |
| `AdditionalCost` | Extra spell costs | `SacrificePermanent(filter)`, `DiscardCards(count)`, `ExileCards(...)` |

`ManaCost` parses string notation into a list of `ManaSymbol` variants — `Colored(color)`,
`Generic(amount)`, `Hybrid(color1, color2)`, `Phyrexian(color)`, `X`, and `Colorless`. This
structured representation enables the solver to reason about payment options without string parsing.

**Tier 2: Mana Solving (Engine).** The `ManaSolver` determines whether a player *can* pay a cost
and *which sources to tap*. This is a constraint satisfaction problem, not a simple subtraction:

```kotlin
class ManaSolver {
    fun canPay(state: GameState, playerId: EntityId, cost: ManaCost, xValue: Int?): Boolean
    fun solve(state: GameState, playerId: EntityId, cost: ManaCost, xValue: Int?): ManaSolution?
}
```

The solver's algorithm:

1. **Inventory available sources.** Find all untapped mana-producing permanents the player controls
   (using projected state — a land animated into a creature with summoning sickness can't tap).
2. **Pay colored costs first.** Colored symbols have no flexibility — `{B}` requires black mana.
   The solver taps the least-flexible source that produces the needed color.
3. **Pay generic costs last.** Generic mana (`{2}`) can use any source. The solver preserves
   flexibility by tapping in priority order:

```
Basic lands (priority 0-1) → Single-color nonbasics (2) → Dual lands (3)
→ Tri lands (4) → Utility lands (+10) → Pain lands (+15) → Creatures (+20)
```

This priority system preserves strategic value — creatures that could attack, utility lands with
activated abilities, and pain lands that cost life are tapped last.

4. **Consider hand requirements.** The solver analyzes the player's hand to avoid tapping sources
   needed for future casts. If you have a red spell and a blue spell in hand with one Mountain and
   one Island, the solver won't tap the Island to pay for the red spell's generic cost.

**Tier 3: Cost Execution (Engine).** The `CostHandler` physically pays costs — tapping permanents,
deducting from the mana pool, sacrificing creatures, discarding cards, paying life. The `ManaPool`
data class is immutable:

```kotlin
data class ManaPool(
    val white: Int = 0, val blue: Int = 0, val black: Int = 0,
    val red: Int = 0, val green: Int = 0, val colorless: Int = 0
) {
    fun canPay(cost: ManaCost): Boolean
    fun pay(cost: ManaCost): ManaPool?  // returns new pool, or null if can't pay
}
```

**Cost reductions and increases.** Before payment, the `CostCalculator` applies all active
modifiers — static abilities that reduce costs (Goblin Electromancer's "instant and sorcery spells
cost {1} less"), affinity ("costs {1} less for each artifact you control"), and cost increases
(Thalia's "noncreature spells cost {1} more"). Colored requirements are never reduced below their
base amount — a `{1}{R}{R}` spell reduced by {2} still costs `{R}{R}`.

**Alternative payments.** Some costs bypass mana entirely — Delve exiles cards from the graveyard
instead of paying generic mana, Convoke taps creatures, and Force of Will can be cast by exiling a
blue card and paying 1 life. The `AlternativePaymentHandler` processes these before the mana solver
runs, reducing the remaining cost that must be paid with mana.

**Why three tiers instead of a single "pay cost" function?**

- **Legal action computation.** The server must determine whether each spell in a player's hand is
  castable *without actually paying*. The solver's `canPay()` method answers this question cheaply.
- **Separation of validation from execution.** The solver determines *if* and *how* to pay; the
  handler *executes* the payment. This prevents partial payment corruption — if a cost can't be
  fully paid, no state changes occur.
- **Auto-pay UX.** Players rarely want to manually choose which lands to tap. The solver's priority
  system produces Arena-quality automatic payment. Manual tapping is available as an opt-in
  `PaymentStrategy.Explicit` for edge cases.

### 2.11 Copy Effects

**Principle:** Copy effects resolve at entry time by replacing the base `CardComponent`, making
the copied identity the starting point for all subsequent layers.

Clone effects are notoriously tricky in MTG engines because they interact with every layer of the
continuous effect system. The engine handles them with an elegant design: copy is a *replacement
effect* that fires before the permanent enters the battlefield, replacing its `CardComponent`
wholesale. By the time the entity exists on the battlefield, it already *is* the copied creature
as far as base state is concerned.

**SDK modeling.** Clone is declared as a replacement effect on the card definition:

```kotlin
val Clone = card("Clone") {
    manaCost = "{3}{U}"
    typeLine = "Creature — Shapeshifter"
    stats(0, 0)

    replacementEffect(
        ReplacementEffect.EntersAsCopy(optional = true)
    )
}
```

`EntersAsCopy` intercepts the zone change to the battlefield. When the engine detects this
replacement effect during spell resolution, it pauses and presents a `SelectCardsDecision` listing
all creatures on the battlefield.

**Engine resolution.** The flow uses the continuation system:

1. **Detection.** During spell resolution, `StackResolver` checks the card's replacement effects
   for `EntersAsCopy`. If found and valid targets exist, it creates a `SelectCardsDecision`.
2. **Pause.** A `CloneEntersContinuation` is pushed onto the continuation stack, capturing the
   spell's entity ID and all resolution context.
3. **Player choice.** The player selects a creature (or declines if the effect is optional).
4. **Component replacement.** On resumption, the `ModalAndCloneContinuationResumer` replaces the
   spell's `CardComponent` with a copy of the target creature's `CardComponent`. A
   `CopyOfComponent` is attached to track the original identity:

```kotlin
data class CopyOfComponent(
    val originalCardDefinitionId: String,   // "Clone"
    val copiedCardDefinitionId: String      // "Elvish Warrior"
) : Component
```

5. **Battlefield entry.** The permanent enters the battlefield with the copied stats, types,
   keywords, and abilities as its base state.

**Why copy is resolved before entry, not as a continuous effect layer.**

Rule 613 defines Layer 1 as the copy layer, applied before all other continuous effects. The engine
satisfies this ordering by resolving copies *at entry time* — the copied `CardComponent` becomes
the base state that Layers 2-7 then modify during state projection. This means:

- **No Layer 1 logic in `StateProjector`.** The projector starts at Layer 2 (control-changing)
  because copy effects are already baked into the base state. This simplifies the projection
  pipeline significantly.
- **Correct layer interactions.** If a Clone copies a creature that has been modified by Giant
  Growth (+3/+3), the Clone gets the creature's *printed* stats (from `CardComponent`), not the
  buffed stats. This is correct per Rule 707.2 — copies copy the "copiable values" (printed
  characteristics), not the current modified values.
- **Clean lifetime management.** When the clone leaves the battlefield and returns (e.g., via
  flickering), it enters as a fresh Clone and can choose a new target. No stale copy state to
  clean up.

The `Layer` enum still includes `COPY` for completeness, but the `StateProjector` doesn't process
it — it exists to maintain a 1:1 mapping with Rule 613's layer numbering.

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
- **Legal action enrichment:** Adding presentation data (mana source info, auto-tap previews) to engine-computed legal actions

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

**Before and after.** Consider a 2/2 creature with a +1/+1 counter that has been enchanted with
an aura granting Flying, controlled by a player who stole it with a control-changing effect:

```
ENGINE (ComponentContainer — queried by type):

  get<CardComponent>()         → name="Grizzly Bears", baseStats=2/2, baseKeywords=[]
  get<ControllerComponent>()   → playerId="player-1"   (base: original controller)
  get<CountersComponent>()     → {PLUS_ONE_PLUS_ONE: 1}
  get<TappedComponent>()       → present (tapped)
  get<DamageComponent>()       → amount=1

  ProjectedState (Rule 613):
    power=3, toughness=3       (2/2 + counter)
    keywords=[FLYING]          (from aura)
    controllerId="player-2"    (control-changing effect)


CLIENT (ClientCard — flat fields, ready to render):

  ClientCard(
    name = "Grizzly Bears",
    power = 3,                 // ← PROJECTED (includes +1/+1 counter)
    toughness = 3,
    basePower = 2,             // ← BASE (for buff/debuff indicators)
    baseToughness = 2,
    keywords = [FLYING],       // ← PROJECTED (includes aura)
    controllerId = "player-2", // ← PROJECTED (includes control change)
    counters = {PLUS_ONE_PLUS_ONE: 1},
    isTapped = true,
    damage = 1,
    manaCost = "{1}{G}",       // ← Rendered string, not ManaCost object
    typeLine = "Creature — Bear",
    // ... all in one flat object
  )
```

The transformer resolves the projected state (applying Rule 613 layers), converts internal types to
rendered strings, and flattens the component bag into direct fields. The client never needs to call
`get<ComponentType>()` or understand the projection system — it receives pre-computed values.

**Why a separate DTO layer?**

- **Decoupling.** Engine refactors (renaming components, restructuring state) don't break the client.
  The transformer absorbs the change.
- **Optimization.** The client DTO includes only what's needed for rendering. Projected stats are
  pre-computed, mana costs are formatted, and irrelevant internal data is stripped. After the first
  full state update, a `StateDiffCalculator` sends only the changed cards, zones, and scalars via
  `StateDeltaUpdate` messages — reducing bandwidth for incremental updates.
- **Versioning.** The DTO contract can be versioned independently of the engine. This enables
  backwards-compatible client updates.

### 3.4 Engine-Authoritative Legal Actions

**Principle:** The engine computes all legal actions; the server enriches them with presentation data.

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

The `LegalActionEnumerator` (in `rules-engine`) evaluates the current game state against the rules
to determine what's legal: which spells can be cast (considering mana, timing, targets), which
abilities can be activated, whether lands can be played, and what combat actions are available. It
delegates to per-category `ActionEnumerator` implementations (spells, abilities, combat, etc.),
mirroring the `ActionHandler` registry pattern used for action execution.

The server's `LegalActionEnricher` then maps the engine's `LegalAction` results to `LegalActionInfo`
DTOs, adding presentation-only data: available mana source information for the pre-cast UI selection.
The client protocol remains unchanged.

```
Engine: LegalActionEnumerator.enumerate(state, playerId) → List<LegalAction>
Server: LegalActionEnricher.enrich(actions, state, playerId) → List<LegalActionInfo>
```

**Why compute legal actions in the engine?**

- **Single source of truth.** Legal action enumeration reuses the same cost calculation, target
  validation, and timing logic as action execution. Keeping both in the engine eliminates the risk
  of the two diverging.
- **AI and simulation.** The engine can now answer "what can this player do?" without a server,
  enabling Monte Carlo Tree Search, automated testing, and headless simulation.
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

**Principle:** Each architectural layer has a dedicated testing approach, and higher layers build on
the guarantees of lower ones.

Testing a Magic rules engine presents a unique challenge: the system must handle tens of thousands
of card interactions, complex rules edge cases, and a full client-server pipeline — but individual
tests must be fast, deterministic, and focused. The engine addresses this with three test layers,
each answering a different question.

### 5.1 Unit and Integration Tests (Engine Layer)

**Question: Does the engine implement individual rules correctly?**

Engine-level tests use Kotest's FunSpec with a `GameTestDriver` that wraps the `ActionProcessor`:

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
isolation. Because the engine is a pure function `(GameState, GameAction) -> ExecutionResult`, tests
can construct any `GameState` directly and assert against the output state — no mocking required.

### 5.2 Scenario Tests (Card Interaction Layer)

**Question: Does each card work correctly in realistic game situations?**

Scenario tests are the primary verification for card correctness. They use `ScenarioTestBase`, which
provides a declarative builder for constructing specific board states:

```kotlin
class GravediggerScenarioTest : ScenarioTestBase() {
    init {
        test("returns creature from graveyard to hand on ETB") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Gravedigger")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Gravedigger")
            game.resolveStack()

            // ETB trigger pauses for target selection
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears")
            game.selectTargets(bears)
            game.resolveStack()

            game.isOnBattlefield("Gravedigger") shouldBe true
            game.isInHand(1, "Grizzly Bears") shouldBe true
        }
    }
}
```

**Why `ScenarioTestBase` instead of raw `ActionProcessor` calls?**

- **Declarative board setup.** Testing Gravedigger's ETB trigger requires a creature in the
  graveyard, lands for mana, and the right phase. With raw `ActionProcessor`, this means manually
  constructing `GameState` with correct `EntityId`s, `ComponentContainer`s, `ZoneKey` mappings,
  and turn state — easily 50+ lines of boilerplate. The builder does this in 6 readable lines.
- **Name-based interaction.** `castSpell(1, "Gravedigger")` finds the card by name, resolves its
  entity ID, constructs `ChosenTarget` objects, and submits the `CastSpell` action. Without this,
  every test would need manual entity ID lookups.
- **Decision handling.** Cards that pause for player input (targeting, yes/no choices, card
  selection) need specific response actions. `ScenarioTestBase` provides `selectTargets()`,
  `answerYesNo()`, `selectCards()`, and `resolveStack()` (which auto-passes priority for both
  players until the stack empties).
- **Card registry integration.** The builder auto-looks up card definitions from registered sets,
  instantiates entities with full `CardComponent` data, and wires static abilities via
  `StaticAbilityHandler` — exactly as the real engine would.

Each card typically has one or more scenario tests covering its main functionality and edge cases.
The `TestGame` wrapper returned by the builder is a thin layer over the real `ActionProcessor` — no
mocked engine, no faked rules.

### 5.3 End-to-End Tests with Dev Scenario API (Full Stack Layer)

**Question: Does the entire pipeline work — server, WebSocket, state transformation, masking, client
rendering, and user interaction?**

E2E tests run against the full stack in a real browser using Playwright. The
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

**Why inject board state via API instead of using seed-based deterministic games?**

A seed-based approach (fixed random seed → deterministic shuffle → play through multiple turns to
reach the desired state) would couple tests to the exact initialization sequence. Any change to
deck construction, mulligan logic, or turn ordering would break every test. Board state injection
decouples the test from all game initialization logic — the test starts at exactly the state it
needs, regardless of how the engine builds that state in a real game.

**Why a Dev Scenario API?**

- **Deterministic setup.** E2E tests need precise board states. Drawing random cards from shuffled
  decks would make tests flaky. The scenario API bypasses randomness entirely.
- **Fast iteration.** Tests start with the exact state they need — no multi-turn setup to reach a
  specific game position.
- **Full-stack coverage.** These tests exercise the entire pipeline: WebSocket handling, state
  transformation, masking, client rendering, user interaction, and action processing.
- **Separated concerns.** The `GamePage` page object encapsulates all UI interaction, so tests read
  like natural-language descriptions of game actions.

**Why three layers instead of just E2E tests?**

E2E tests take seconds per test (browser startup, WebSocket handshake, DOM rendering). Scenario
tests take milliseconds. Unit tests take microseconds. The test pyramid ensures that the vast
majority of rules correctness (hundreds of card interactions, thousands of edge cases) is verified
at the fast scenario layer, while E2E tests focus on the smaller set of integration concerns (does
the UI render the right thing? does state masking hide the right information? do WebSocket messages
arrive correctly?). This keeps the full test suite runnable in minutes, not hours.

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
| Turn structure with priority gating all actions | Passed-by set + step state machine |
| Mana payment is a constraint satisfaction problem | Three-tier cost system + smart solver |
| Copies interact with every continuous effect layer | Entry-time replacement + base state swap |
| 20,000+ unique cards | Data-driven definitions + DSL |
| Library manipulation has infinite variants | Atomic effect pipelines (Gather/Select/Move) |
| Dynamic values that change constantly | AST-based DynamicAmount |
| Multiplayer with hidden information | Server-authoritative state + masking |
| Client-server state divergence | Dumb terminal pattern |
| Complex legal action computation | Engine-side enumeration + server enrichment |
| Multi-game tournaments with drafting | State machine lobby + eager match starting |
| Testing edge cases across the stack | Dev Scenario API + multi-layer test pyramid |

The result is a system where each layer has a clear, minimal responsibility — the SDK describes,
the engine executes, the server orchestrates, and the client renders.