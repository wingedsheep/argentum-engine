# Modular MTG Game Engine Architecture

## 1. Core Philosophy

This architecture is built on **Domain-Driven Design (DDD)** principles fused with an **Entity-Component-System (ECS)** state model. It prioritizes strict separation of concerns to ensure the engine remains "pure" (containing no card-specific code), while the content (cards/sets) remains "dumb" (containing no execution logic).

### Key Architectural Pillars

1. **The "Shared Contract" (SDK)**: Defines the language of Magic (what a "Cost" is, what "Damage" means) without defining how to process it.
2. **The "Data-Driven" Content**: Cards are defined as configuration data via a type-safe DSL, not as executable classes.
3. **The "Pure" Engine**: A stateless processor that takes a `GameState` + `Action`, applies rules, and returns a `NewGameState` + `Events`.
4. **The "Anti-Corruption" Layer (API)**: A strict boundary that prevents internal engine refactors from breaking the game server or frontend client.

---

## 2. High-Level Module Structure

The project is divided into four distinct modules (gradle subprojects):

```text
/project-root
│
├───mtg-sdk/            # [SHARED] The primitives, data models, and DSL definitions.
│                       # Dependencies: None.
│
├───mtg-engine/         # [LOGIC] The rules engine, game loop, and state management.
│                       # Dependencies: mtg-sdk.
│
├───mtg-sets/           # [CONTENT] The actual card definitions (Alpha, Portal, Custom).
│                       # Dependencies: mtg-sdk.
│
└───mtg-api/            # [SERVER] The public interface for game servers/clients.
                        # Dependencies: mtg-engine, mtg-sdk.

```

---

## 3. Module Breakdown

### A. `mtg-sdk` (The Shared Contract)

**Role:** The vocabulary of the game. It defines the "shape" of data but contains no execution logic.

#### File Structure

```text
com.wingedsheep.mtg.sdk
├───core                        <-- DOMAIN PRIMITIVES
│   ├───enums                   (Color, ZoneType, Phase, Step)
│   ├───keyword                 (Keyword.kt, KeywordDefinition.kt)
│   └───values                  (ManaCost, CreatureStats, TypeLine)
│
├───model                       <-- DATA MODELS
│   ├───CardDefinition.kt       (The Master Object: Rules + Metadata)
│   ├───CardMetadata.kt         (Images, Flavor Text, Rarity)
│   └───CardScript.kt           (Container for game logic configurations)
│
├───scripting                   <-- LOGIC BUILDING BLOCKS (Data Classes)
│   ├───ability                 (ActivatedAbility, TriggeredAbility, StaticAbility)
│   ├───effect                  (DealDamage, DrawCards, DestroyPermanent, AddMana)
│   ├───cost                    (ManaCost, SacrificeCost, TapCost, LifeCost)
│   ├───condition               (LifeTotalCondition, ControlPermanentCondition)
│   ├───trigger                 (OnEnterBattlefield, OnDeath, OnPhaseBegin)
│   └───target                  (TargetFilter, SelectionMode)
│
├───dsl                         <-- KOTLIN BUILDERS
│   ├───CardBuilder.kt          (Backing for `card("Name") { }`)
│   ├───AbilityBuilder.kt       (Backing for `activated { }`)
│   └───SetBuilder.kt           (Backing for `cardSet("CODE") { }`)
│
└───spi                         <-- SERVICE PROVIDER INTERFACE
    └───CardSetProvider.kt      (The Plugin Interface)

```

#### Key Concept: Split Definition

The `CardDefinition` holds data for *both* the Engine and the Frontend, but they are compartmentalized.

```kotlin
// sdk/model/CardDefinition.kt
@Serializable
data class CardDefinition(
    val id: String,             // e.g., "por-grizzly-bears"
    
    // Engine Data (Lean)
    val cost: ManaCost,
    val typeLine: TypeLine,
    val stats: CreatureStats?,
    val script: CardScript?,    // Logic definitions (Effects/Costs)

    // Frontend Data (Rich)
    val metadata: CardMetadata  // Artist, ImageURI, FlavorText, Rarity
)

```

---

### B. `mtg-sets` (The Content)

**Role:** A collection of data definitions. It utilizes the SDK to define cards. It does not know about the Engine.

#### File Structure

```text
com.wingedsheep.mtg.sets
├───provider                    <-- REGISTRATION
│   └───SetRegistry.kt          (ServiceLoader implementation)
│
└───definitions                 <-- THE CARDS
    ├───portal
    │   └───PortalSet.kt        (Defines Portal cards)
    ├───alpha
    │   └───AlphaSet.kt         (Defines Alpha cards)
    └───custom
        └───DragonsSet.kt       (Defines custom cards)

```

#### Example Content File (`PortalSet.kt`)

```kotlin
val PortalSet = cardSet("POR", "Portal") {
    // Simple Creature
    card("Grizzly Bears") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        
        // Metadata (Ignored by engine, used by frontend)
        metadata {
            rarity = Rarity.COMMON
            flavorText = "We cannot go to the woods today..."
            artist = "D. J. Cleland-Hura"
        }
    }

    // Spell with Logic
    card("Lava Axe") {
        manaCost = "{4}{R}"
        typeLine = "Sorcery"
        
        spell {
            effect = Effects.DealDamage(5)
            target = TargetFilter.PlayerOrPlaneswalker
        }
    }
}

```

---

### C. `mtg-engine` (The Logic Core)

**Role:** The simulator. It loads the `sets`, maintains the `state`, and executes the `rules`.

#### File Structure

```text
com.wingedsheep.mtg.engine
├───state                       <-- ECS DATA MODEL (Pure State)
│   ├───GameState.kt            (Immutable snapshot: entities, zones, turn)
│   ├───EntityId.kt
│   ├───ComponentContainer.kt
│   └───components              (Grouped by domain)
│       ├───identity            (CardComponent, ControllerComponent)
│       ├───battlefield         (TappedComponent, CountersComponent)
│       ├───combat              (AttackingComponent, BlockingComponent)
│       └───stack               (SpellOnStackComponent)
│
├───core                        <-- ORCHESTRATION
│   ├───GameLoop.kt             (Phase/Step transitions)
│   ├───ActionProcessor.kt      (Request -> Validation -> Mutation -> Event)
│   └───TurnManager.kt          (Turn-based actions: Untap, Draw)
│
├───mechanics                   <-- RULES IMPLEMENTATION
│   ├───layers                  (Rule 613: StateProjector, ModifierProvider)
│   ├───combat                  (CombatValidator, DamageCalculator)
│   ├───stack                   (StackResolver, TargetValidator)
│   ├───mana                    (ManaPool, PaymentLogic)
│   └───replacement             (ReplacementEffectResolver)
│
├───handlers                    <-- SCRIPT EXECUTORS
│   ├───EffectHandler.kt        (Switch statement mapping SDK Effects to Logic)
│   ├───CostHandler.kt          (Validates and pays SDK Costs)
│   └───ConditionEvaluator.kt   (Checks SDK Conditions against GameState)
│
└───loader                      <-- DYNAMIC LOADING
    └───SetLoader.kt            (Scans classpath for CardSetProviders)

```

#### Key Concept: Effect Execution

The engine maps the "dumb" data objects from the SDK to "smart" logic functions.

```kotlin
// engine/handlers/EffectHandler.kt
fun execute(state: GameState, effect: Effect, context: EffectContext): GameState {
    return when(effect) {
        is Effect.DealDamage -> DamageLogic.apply(state, effect.amount, context.target)
        is Effect.DrawCards -> LibraryLogic.draw(state, context.player, effect.count)
        // New effects are added here
    }
}

```

---

### D. `mtg-api` (Server Interface)

**Role:** The facade. It translates external requests (from a WebSocket/HTTP server) into internal engine actions and exposes data views to the client.

#### File Structure

```text
com.wingedsheep.mtg.api
├───service                     <-- FACADES
│   ├───GameService.kt          (Runtime game management)
│   └───CardCatalogService.kt   (Static data for deckbuilding)
│
├───request                     <-- INPUT DTOs (Client -> Server)
│   ├───GameRequest.kt          (Sealed: CastSpell, PassPriority, MakeDecision)
│   └───AdminRequest.kt         (ForceState, Debug)
│
├───event                       <-- OUTPUT EVENTS (Server -> Client)
│   ├───GameEvent.kt            (Public events: ZoneChange, LifeChange)
│   └───AnimationEvent.kt       (Hints: "Card X flew to Stack")
│
└───view                        <-- VIEW DTOs (Read-Only)
    ├───CardView.kt             (JSON-ready card data)
    ├───GameStateView.kt        (Sanitized state for a specific player)
    └───ZoneView.kt

```

---

## 4. Data Flow & Lifecycles

### A. Initialization Lifecycle

1. **Server Start**: `CardCatalogService` initializes `SetLoader`.
2. **Discovery**: `SetLoader` uses Java `ServiceLoader` to find all compiled JARs implementing `CardSetProvider` (e.g., `mtg-sets-portal.jar`).
3. **Registration**: Cards are indexed. Frontend requests `/api/cards` to get the JSON `CardView` list (with images/text) for the deckbuilder.

### B. Runtime Game Loop

1. **Input**: Server receives `CastSpellRequest(cardId="uuid-1", target="uuid-2")`.
2. **API Layer**: Converts Request to Engine call.
3. **Engine (Validator)**: Checks `StackResolver.validateCast()`.
* Is priority held?
* Can costs be paid?
* Are targets valid?


4. **Engine (Processor)**:
* Moves card to Stack Zone.
* Subtracts Mana.
* Creates `SpellOnStack` entity.


5. **Output**: Engine emits `GameEvent.SpellCast`.
6. **API Layer**: Server broadcasts update to clients.

### C. Frontend Rendering

1. Client receives `GameEvent` containing `EntityId("uuid-1")` and `CardId("por-lava-axe")`.
2. Client looks up `por-lava-axe` in its local Catalog cache (downloaded at startup).
3. Client renders the image and text associated with that Card ID.

---

## 5. Extensibility Guide

### Adding a New Mechanic (e.g., "Scry")

1. **SDK**: Add `data class Scry(val amount: Int) : Effect` to `Effects.kt`.
2. **Engine**: Add handling logic in `EffectHandler.kt` -> `is Effect.Scry -> LibraryLogic.scry(...)`.
3. **Sets**: Use it. `effect = Effects.Scry(2)`.

### Adding a New Set

1. Create a new file in `mtg-sets/definitions/`.
2. Implement `CardSetProvider`.
3. Define cards using the DSL.
4. *No Engine compilation required.*

### Adding a New Rule (e.g., "Legend Rule")

1. **Engine**: Add a `StateBasedAction` in `StateBasedActions.kt`.
2. It checks the ECS components (`TypeLine` contains "Legendary", `Controller` matches).
3. It generates a `Sacrifice` action if violated.
