# Managing Complex and Rare Abilities in an ECS Engine

Managing complex, unique, or "one-off" abilities in an Entity-Component-System (ECS) architecture is a major design
challenge. The goal is to prevent card-specific logic from leaking into the core engine while maintaining a data-driven,
serializable state.

This document outlines a hierarchy of strategies, ranging from simple composition to advanced engine hooks.

---

## 1. Composition (The "Lego" Approach)

Most "complex" abilities are actually specific combinations of simple, existing actions. Instead of creating a new
effect class for every variation, compose them using structural effects.

* **Strategy:** Use `CompositeEffect` (sequence) and `ConditionalEffect` (branching) to chain existing primitives.
* **Best For:** Abilities with multiple steps or simple conditions.

### Example: *Cryptic Command* (Simplified Mode)

*"Return target permanent to its owner's hand. Draw a card."*

Instead of `BounceAndDrawEffect`, script it as:

```kotlin
CompositeEffect(
    effects = listOf(
        ReturnToHandEffect(EffectTarget.TargetPermanent),
        DrawCardsEffect(count = 1, target = EffectTarget.Controller)
    )
)

```

### Example: *Doom Blade / Terror Variation*

*"Destroy target creature if it's black. Otherwise, tap it."*

```kotlin
ConditionalEffect(
    condition = TargetHasColor(Color.BLACK),
    effect = DestroyEffect(EffectTarget.TargetCreature),
    elseEffect = TapEffect(EffectTarget.TargetCreature)
)

```

---

## 2. New Components as "Markers" (State Tracking)

Some abilities change the *status* of an object or player permanently or for a duration. In ECS, status is state, and
state belongs in **Components**.

* **Strategy:** Define a specific, often empty, **Tag Component** to mark the entity or player.
* **Best For:** Statuses like "Monstrous", "The City's Blessing", or rules modifications like "You can't lose the game."

### Example: *Platinum Angel*

*"You can't lose the game and your opponents can't win the game."*

1. **Define Component:** `data class CantLoseGameComponent(val sourceId: EntityId) : Component`
2. **Script:** Add a `StaticAbility` that grants this component to the controller.
3. **Engine Hook:** Update the core logic in `GameActionHandler` to check for this component.

```kotlin
// In GameActionHandler.kt
private fun executePlayerLoses(state: GameState, action: PlayerLoses): GameState {
    if (state.hasComponent<CantLoseGameComponent>(action.playerId)) {
        return state // Loss prevented
    }
    // ... proceed with normal loss logic
}

```

---

## 3. The "Custom Effect" Pattern (The Extension Point)

When an ability performs a unique game action that doesn't exist in the engine yet (e.g., "Exchange life totals", "
Restart the game"), you must extend the system.

* **Strategy:** Follow the Open-Closed Principle. Create a new `Effect` data class and a corresponding `EffectHandler`.
* **Best For:** Truly unique actions that cannot be composed from existing primitives.

### Example: *Tree of Redemption*

*"Exchange your life total with this creature's toughness."*

1. **Define Data:** `data class ExchangeLifeWithToughnessEffect(val target: EffectTarget) : Effect`
2. **Implement Logic:** Create `ExchangeLifeWithToughnessHandler` that implements `EffectHandler`.
3. **Register:** Add the handler to the `EffectHandlerRegistry`.

*Benefit:* The core `GameEngine` code remains untouched. The logic is isolated entirely within the handler.

---

## 4. Parameterized Custom Logic (Logic Registry)

Sometimes multiple cards perform similar complex math or logic, but the variables differ (e.g., *Tarmogoyf*, *Consuming
Aberration*). Creating a unique class for every card is sustainable.

* **Strategy:** Create a generic effect (like `ScriptedMathEffect`) that accepts an ID or parameters, and looks up the
  logic implementation in a dedicated registry.
* **Best For:** Characteristic-Defining Abilities (CDAs) or complex value calculations.

### Example: *Tarmogoyf*

*"Power is equal to the number of card types among cards in all graveyards..."*

1. **Script:** Use `SetPTFromCDA(CDAType.CUSTOM, "TarmogoyfLogic")`.
2. **State Projector:** When the projector encounters `CDAType.CUSTOM`, it queries a `CustomLogicRegistry` using the
   string ID.
3. **Implementation:** The registry contains a lambda that calculates the value based on the current `GameState`.

---

## 5. Layer System Hooks (Rule Modifications)

Cards that fundamentally change game rules often interact with the Layer System (Rule 613).

* **Strategy:** Define new `Modification` types for the `StateProjector`.
* **Best For:** Abilities that remove other abilities, change how combat works, or alter text/colors.

### Example: *Archetype of Imagination*

*"Creatures your opponents control lose flying and can't have or gain flying."*

1. **Existing Tool:** We have `Modification.RemoveKeyword`.
2. **New Requirement:** We need `Modification.CantHaveKeyword(Keyword.FLYING)`.
3. **Update Projector:**

* Apply `CantHaveKeyword` modifiers in **Layer 6**.
* When applying `AddKeyword` modifiers later in the timestamp order, the Projector checks:
  `if (!entity.hasRestriction(CantHaveKeyword(FLYING))) { entity.add(FLYING) }`

---

## 6. The "Nuclear Option": Replacement Effect System

This is for cards that interrupt or replace fundamental events in complex ways.

* **Strategy:** Implement an Observer/Interceptor pattern where the engine emits "Proposed Events" before "Actual
  Events."
* **Best For:** Prevention effects, doubling effects (*Doubling Season*), or replacement effects (*Word of Command*).

### Example: *Doubling Season*

*"If an effect would create one or more tokens... create twice that many."*

1. **Proposal:** Action is proposed: `GameAction.AddCounters(entity, 2)`.
2. **Query:** Engine checks a `ReplacementEffectSystem`.
3. **Intercept:** *Doubling Season* (via its component) sees the event type matching its criteria.
4. **Modify:** It replaces the action with `GameAction.AddCounters(entity, 4)`.
5. **Execute:** Engine executes the modified action.

---

## Summary Matrix

| Complexity Level       | Strategy                                 | Representative Card |
|------------------------|------------------------------------------|---------------------|
| **High Logic**         | **Composition:** `CompositeEffect`       | *Cryptic Command*   |
| **Unique State**       | **Component:** New Marker Component      | *City of Blessing*  |
| **Unique Action**      | **Extension:** New `Effect` & `Handler`  | *Shahrazad*         |
| **Rule Change**        | **Engine Hook:** Check for Component     | *Platinum Angel*    |
| **Math/Values**        | **Registry:** `CustomCDA` / Logic Lookup | *Tarmogoyf*         |
| **Event Modification** | **Interceptor:** Replacement Effects     | *Doubling Season*   |

This approach ensures the engine remains **modular**. You avoid modifying the monolithic `GameEngine.kt` file for every
new card set; instead, you expand the system horizontally by adding new data types and handlers.
