# The Perfect MTG ECS Architecture

This document describes a "North Star" architecture for a Magic: The Gathering game engine. It moves away from
traditional Object-Oriented Programming (OOP) inheritance hierarchies (e.g., `Creature extends Permanent`) toward a
pure, data-driven ECS model.

## 1. Core Philosophy

The "Perfect" system is built on three pillars:

1. **Immutability:** The entire `GameState` is an immutable data structure. State changes are created by reducing
   Actions into a new State. This enables trivial undo/redo, reliable networking (state diffing), and safe AI
   simulation (Monte Carlo Tree Search).
2. **Strict Separation:**

* **Entities** are just IDs.
* **Components** are pure data (no methods).
* **Systems** contain all logic.


3. **Derived State (The Layer System):** The game state stored on disk/memory is the "Base State." The state the players
   see and rules interact with is the "Projected State," calculated dynamically by applying MTG Rule 613 (Layers).

---

## 2. The Entity

In this system, an **Entity** is nothing more than a unique identifier.

```kotlin
@JvmInline
value class EntityId(val value: String)

```

Everything is an entity:

* **Players**
* **Cards** (in hand, library, graveyard)
* **Permanents** (on the battlefield)
* **Spells/Abilities** (on the stack)
* **Emblems**
* **Continuous Effects** (floating modifiers like "Giant Growth")

---

## 3. The Components (Data)

Components are granular data bags. An entity is defined solely by the composition of its components.

### Identity & Core

* `CardDefinitionComponent`: Reference to the immutable Oracle data (Name, Mana Cost, Types).
* `ControllerComponent`: `playerId`.
* `OwnerComponent`: `playerId`.

### State & Status

* `TappedComponent`: (Tag) Present if tapped.
* `CountersComponent`: Map of `CounterType -> Int`.
* `DamageMarkedComponent`: `Int` (Cleared during cleanup).
* `SummoningSicknessComponent`: (Tag) Removed at start of turn.
* `FaceDownComponent`: For morphs/manifests.

### Combat

* `AttackingComponent`: Target (Player/Planeswalker).
* `BlockingComponent`: Target (Attacker ID).
* `BlockedByComponent`: List of blocker IDs (Damage assignment order).

### The Stack

* `SpellOnStackComponent`: Targets, Modes, Kicker status, X value.
* `TriggeredAbilityComponent`: Source ID, Trigger Context.

### Player Specific

* `LifeComponent`: `Int`.
* `ManaPoolComponent`: `ManaPool`.
* `GraveyardComponent`: List of EntityIds.

---

## 4. The Systems (Logic)

In a turn-based card game, "Systems" are primarily **Action Handlers** and **State Projectors**.

### A. The Action Reducer (The Heart)

The engine functions as a state machine. It accepts a `GameAction` and the current `GameState`, produces a new
`GameState`, and emits `GameEvents`.

**Signature:**
`(GameState, GameAction) -> ExecutionResult(GameState, List<GameEvent>)`

**Key Actions:**

* `CastSpell`
* `PassPriority`
* `DeclareAttackers`
* `ResolveStack`

### B. The State Projector (The Brain)

MTG requires continuous effects (Rule 613). The state stored in `components` is the "Base State." The Projector
calculates the "Actual State."

**Logic Flow:**

1. **Start:** Take Base State (Card Definition).
2. **Layer 1 (Copy):** Apply copy effects.
3. **Layer 2 (Control):** Apply control change effects.
4. **Layer 3 (Text):** Apply text-changing effects.
5. **Layer 4 (Type):** Apply type-changing effects.
6. **Layer 5 (Color):** Apply color-changing effects.
7. **Layer 6 (Ability):** Add/Remove keywords and abilities.
8. **Layer 7 (P/T):**

* 7a: CDAs (e.g., Tarmogoyf).
* 7b: Set P/T (e.g., Sorceress Queen).
* 7c: Modify P/T (e.g., Giant Growth).
* 7d: Counters (+1/+1).
* 7e: Switch P/T.


9. **Output:** A `GameObjectView` used for all rule checks.

### C. The Trigger Detector (The Nerves)

Listens to `GameEvents` emitted by the Action Reducer.

1. Input: `GameEvent` (e.g., `CreatureDied`).
2. Process: Scan all active entities for `TriggeredAbility` definitions matching the event.
3. Output: List of `PendingTrigger` objects to be put on the stack.

---

## 5. Scripting & extensibility

Cards are not classes. They are data definitions with attached scripts.

### The Card Script DSL

A domain-specific language (DSL) defines card behavior cleanly.

```kotlin
card("Lightning Bolt") {
    manaCost("{R}")
    type("Instant")
    spell {
        target(AnyTarget)
        effect(DealDamage(3))
    }
}

card("Giant Growth") {
    manaCost("{G}")
    type("Instant")
    spell {
        target(Creature)
        // Creates a Modifier in Layer 7c
        effect(ModifyStats(3, 3, Duration.EndOfTurn))
    }
}

card("Clone") {
    manaCost("{3}{U}")
    type("Creature", "Shapeshifter")
    // Replacement effect modifying the 'EntersBattlefield' event
    replacementEffect(EntersBattlefield) {
        choice("Choose a creature to copy") {
            copyEntity(target)
        }
    }
}

```

---

## 6. Managing Complexity

To keep the engine pure, complex logic is handled via the **Extension Pattern**:

1. **Composite Effects:** Build complex spells by chaining simple atoms (`DrawCard`, `Discard`, `Damage`).
2. **Replacement Effects System:** An interceptor layer that runs *before* actions are executed (e.g., Doubling Season
   intercepts `AddCounterAction`).
3. **Custom Handlers:** For unique mechanics (e.g., *Shahrazad* sub-games), write a specific `EffectHandler` rather than
   polluting the core `GameAction` enum.

## 7. Example Workflow: Casting a Spell

1. **Player Decision:** UI sends `RequestCast(CardId, Targets)`.
2. **Validation:** `LegalActionCalculator` checks mana, timing, and targets using `StateProjector` (to ensure targets
   are valid in the current layer state).
3. **Action:** Engine executes `CastSpellAction`.

* Moves card entity from Hand Zone to Stack Zone.
* Adds `SpellOnStackComponent`.
* Deducts mana from `ManaPoolComponent`.


4. **Events:** Emits `SpellCastEvent`.
5. **Triggers:** `TriggerDetector` sees `SpellCastEvent`, checks for "Whenever you cast...", and queues triggers.
6. **State Update:** Returns new immutable `GameState`.
7. **Priority:** Engine checks State-Based Actions (SBAs), then grants priority.
