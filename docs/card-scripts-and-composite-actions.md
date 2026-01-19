# Card Scripts & Composite Actions

This document details the **Card Script DSL**, the core system used to define card behaviors in the Argentum Engine. It
focuses on how to write expressive, composable abilities using the `CompositeEffect` pattern and explicit target
binding.

---

## 1. The Card Script Philosophy

In Argentum Engine, cards are **data**, not classes. Instead of extending a base class (like
`class LightningBolt extends Card`), you define a card's behavior using a **Script**.

A `CardScript` is a pure data structure that contains:

* **Card Name:** Links the script to the `CardDefinition`.
* **Abilities:** Triggered, Activated, and Static abilities.
* **Effects:** The actions that happen when a spell resolves.
* **Targets:** The requirements for selecting targets.

### The DSL (Domain Specific Language)

We use a type-safe Kotlin DSL to write scripts that read almost like English card text.

```kotlin
// Example: Giant Growth
val script = cardScript("Giant Growth") {
    // 1. Define targets
    val creature = targets(TargetCreature)

    // 2. Define spell effect
    spell(
        // 3. Bind effect to specific target
        ModifyStatsEffect(3, 3, ContextTarget(creature.index))
    )
}

```

---

## 2. Targeting: Explicit Binding

A major feature of the engine is **Explicit Target Binding**. Unlike older systems that implicitly guess which target
applies to which effect, we explicitly link them.

### Step 1: Define the Requirement

Use the `targets()` builder method to define what the player must choose. This returns a `TargetReference` (an index).

```kotlin
val targetRef = targets(TargetCreature(count = 1))

```

### Step 2: Use the Reference

Pass this reference to the effect using `EffectTarget.ContextTarget(index)`.

```kotlin
DestroyEffect(EffectTarget.ContextTarget(targetRef.index))

```

This disambiguation is critical for cards with multiple targets, like *Decimate* ("Destroy target artifact and target
land").

---

## 3. Composite Actions (Chaining Effects)

Complex cards often perform multiple actions in sequence. The `CompositeEffect` allows you to chain pure, atomic effects
into a single resolution flow.

### The `then` Operator

The `infix fun then` operator provides a readable syntax for chaining effects.

#### Example: *Recoil*

*"Return target permanent to its owner's hand, then that player discards a card."*

```kotlin
val script = cardScript("Recoil") {
    // Define target
    val permanentRef = targets(TargetPermanent)

    spell(
        // Effect 1: Bounce
        ReturnToHandEffect(ContextTarget(permanentRef.index))

                // Operator: Chain effects
                then

                // Effect 2: Discard (targets the controller of the bounced card)
                DiscardCardsEffect(1, EffectTarget.TargetController)
    )
}

```

### Why Composition?

1. **Reusability:** We don't need a unique `BounceAndDiscardEffect` class. We reuse the existing atomic
   `ReturnToHandEffect` and `DiscardCardsEffect`.
2. **State Updates:** The engine executes effects sequentially. The state updates after the first effect before the
   second begins. This is crucial for rules (e.g., if the first effect destroys a creature, the second effect sees it in
   the graveyard).
3. **Event generation:** Each atomic effect generates its own events (`PermanentReturnedToHand`, `CardDiscarded`),
   ensuring the game log and triggers are accurate.

---

## 4. Advanced Patterns

### Conditional Effects

For "If [condition], do [X], otherwise do [Y]" logic, use `ConditionalEffect` or the `spell()` builder overload.

**Example: *Searing Blaze***
*"Searing Blaze deals 1 damage to target player and 1 damage to target creature. If you played a land this turn, it
deals 3 damage to that player and 3 damage to that creature instead."*

```kotlin
val script = cardScript("Searing Blaze") {
    val player = targets(TargetPlayer)
    val creature = targets(TargetCreature)

    spell(
        // Condition: Landfall (conceptually)
        condition = LandPlayedThisTurn,

        // "True" branch (Landfall active)
        effect = DealDamageEffect(3, ContextTarget(player.index))
                then DealDamageEffect(3, ContextTarget(creature.index)),

        // "False" branch (Normal)
        elseEffect = DealDamageEffect(1, ContextTarget(player.index))
                then DealDamageEffect(1, ContextTarget(creature.index))
    )
}

```

### Optional Targets (Up to X)

Use the `optional = true` flag in `TargetRequirement`.

**Example: *Rooftop Percher***
*"Exile up to two target cards from graveyards."*

```kotlin
val script = cardScript("Rooftop Percher") {
    val graveyardCards = targets(
        TargetCardInGraveyard(
            count = 2,
            optional = true // Allows selecting 0, 1, or 2 cards
        )
    )

    triggered(
        trigger = OnEnterBattlefield(),
        effect = ExileEffect(ContextTarget(graveyardCards.index))
    )
}

```

### Keyword Macros

Common keywords that include rules baggage (like *Prowess*) have builder macros to keep scripts clean.

```kotlin
cardScript("Monastery Swiftspear") {
    // Adds the PROWESS keyword AND the triggered ability logic automatically
    prowess()

    // Standard keyword
    keywords(Keyword.HASTE)
}

```

---

## 5. Summary of Effect Types

| Effect Class            | Description                                   |
|-------------------------|-----------------------------------------------|
| **Life**                |                                               |
| `GainLifeEffect`        | Player gains life.                            |
| `LoseLifeEffect`        | Player loses life.                            |
| `DrainEffect`           | Deal damage and gain that much life.          |
| **Damage**              |                                               |
| `DealDamageEffect`      | Deal damage to any target.                    |
| `DealDamageToAllEffect` | Mass damage (Earthquake, etc.).               |
| **Zones**               |                                               |
| `DrawCardsEffect`       | Player draws cards.                           |
| `DiscardCardsEffect`    | Player discards cards.                        |
| `SearchLibraryEffect`   | Tutor for cards (to Hand, Battlefield, etc.). |
| `ShuffleLibraryEffect`  | Shuffle a library.                            |
| `ReturnToHandEffect`    | Bounce a permanent.                           |
| `ExileEffect`           | Exile a permanent or card.                    |
| `DestroyEffect`         | Destroy a permanent.                          |
| **Stats/State**         |                                               |
| `ModifyStatsEffect`     | +/- P/T (e.g. Giant Growth).                  |
| `AddCountersEffect`     | Add +1/+1 (or other) counters.                |
| `TapUntapEffect`        | Tap or Untap a permanent.                     |
| `GrantKeyword...`       | Give a keyword until end of turn.             |
| **Meta**                |                                               |
| `CompositeEffect`       | Chain multiple effects (`A then B`).          |
| `ConditionalEffect`     | `if (X) A else B`.                            |

This architecture ensures that adding a new set is simply a matter of defining new Data (Cards) and Scripts, rarely
requiring changes to the core Engine code.
