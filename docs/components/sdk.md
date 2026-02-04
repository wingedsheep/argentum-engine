# Argentum SDK (`mtg-sdk`) Developer Documentation

The `mtg-sdk` module is the **Single Source of Truth** for the Argentum Engine. It defines the vocabulary, data structures, and DSLs used to describe Magic: The Gathering cards.

---

## 1. Core Philosophy

The SDK follows a **"Configuration over Code"** architecture. Its primary responsibility is to provide the data models that describe **WHAT** a card does, without defining **HOW** it is executed.

* **Pure Data:** Every object in the SDK is an immutable data class or a sealed interface.
* **Zero Logic:** The SDK contains no rules logic. It doesn't know how to "deal damage"; it only knows how to store the instruction `DealDamage(3)`.
* **Content-Neutral:** The SDK is agnostic to specific card sets. It provides the building blocks for Alpha, Portal, or custom Lorwyn Eclipsed sets equally.

---

## 2. Architecture & Components

The SDK is organized into three main layers: **Core Primitives**, **The Model**, and **The Scripting System**.

### A. Core Primitives (`com.wingedsheep.sdk.core`)

These are the atomic units of MTG.

* **Enums:** `Color`, `Phase`, `Step`, `ZoneType`, `CardType`, `Supertype`.
* **Value Classes:** `ManaCost` (parsed from strings like `"{2}{U}"`), `ManaSymbol`, `Subtype`.
* **TypeLine:** A structured representation of a card's types (e.g., *Legendary Creature — Human Wizard*).

### B. The Model (`com.wingedsheep.sdk.model`)

The high-level containers for game entities.

* **`CardDefinition`:** The master object. It contains static metadata (artist, flavor text, image URIs) and the `CardScript`.
* **`CardScript`:** The behavioral engine of a card. It holds lists of abilities, effects, and restrictions.
* **`CreatureStats`:** Power and toughness, supporting both **Fixed** (2/2) and **Dynamic** values (Tarmogoyf's `*/*`).
* **`EntityId`:** A unified UUID system used for cards, players, and triggers.

### C. The Scripting System (`com.wingedsheep.sdk.scripting`)

This is the "Lego" set used to build card behavior. It uses a compositional pattern:

* **`Trigger`:** Defines **WHEN** (e.g., `OnEnterBattlefield`, `OnDeath`).
* **`Effect`:** Defines **WHAT** (e.g., `DealDamageEffect`, `DrawCardsEffect`).
* **`Condition`:** Defines **IF** (e.g., `LifeTotalAtMost(5)`).
* **`TargetRequirement`:** Defines **WHO/WHICH** (e.g., `TargetCreature`, `TargetPlayer`).

---

## 3. The Unified Filtering System

The SDK uses a powerful, predicate-based filtering system located in `filters/unified/`. This replaces old, hard-coded lists with composable logic.

| Filter | Purpose | Example |
| --- | --- | --- |
| **`GameObjectFilter`** | Selects cards/permanents based on properties. | `Creature.withColor(RED).tapped()` |
| **`TargetFilter`** | Adds zone context to a `GameObjectFilter`. | `Creature.inZone(Zone.Graveyard)` |
| **`GroupFilter`** | Used for mass effects ("All...", "Each..."). | `AllCreatures.youControl()` |

---

## 4. Code Style & Standards

### Immutability & Serialization

All data classes must be **immutable** (`val` only) and annotated with `@Serializable`. This ensures the game state can be sent over WebSockets or saved to a database without transformation.

```kotlin
@Serializable
data class ModifyStatsEffect(
    val powerModifier: Int,
    val toughnessModifier: Int,
    val target: EffectTarget,
    val duration: Duration = Duration.EndOfTurn
) : Effect

```

### Sealed Interfaces

We use `sealed interface` for all extensible types (Effects, Triggers, Conditions). This forces the Engine's `when` statements to be exhaustive, ensuring that if a new Effect is added to the SDK, the Engine won't compile until it's handled.

### Fluent Builders

The SDK provides a "Fluent" API style for filters to make card definitions readable:
`GameObjectFilter.Creature.youControl().withKeyword(FLYING)`

---

## 5. How to Use the SDK (DSL)

To define a card, you use the Kotlin DSL provided by the SDK. This is the bridge between the data structures and human-readable card text.

### Example: Lightning Bolt

```kotlin
val LightningBolt = card("Lightning Bolt") {
    manaCost = "{R}"
    typeLine = "Instant"
    spell {
        effect = Effects.DealDamage(3)
        target = Targets.Any // AnyTarget(count = 1)
    }
}

```

### Example: Flametongue Kavu (ETB Trigger)

```kotlin
val FlametongueKavu = card("Flametongue Kavu") {
    manaCost = "{3}{R}"
    typeLine = "Creature — Kavu"
    power = 4
    toughness = 2
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.DealDamage(4)
        target = Targets.Creature
    }
}

```

---

## 6. Developer Responsibilities

### What goes in the SDK?

* New `Effect` types (e.g., "Scry", "Exile until leaves").
* New `Trigger` types (e.g., "Whenever you cast a second spell").
* New `Keyword` definitions.
* Data models for new card types (e.g., Planeswalkers, Battles).

### What stays OUT of the SDK?

* **Execution Logic:** Code that actually changes a player's life or moves a card belongs in `rules-engine`.
* **State Management:** The current game board lives in `rules-engine`.
* **Network Logic:** WebSocket handlers belong in `game-server`.
