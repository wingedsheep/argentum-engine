# Decompose AmplifyEffect into Pipeline

## Problem

`AmplifyEffect` is a set-specific replacement effect (Legions) that hardcodes "reveal cards from hand sharing a creature type, get N counters per reveal." This is a multi-step flow that should compose from existing pipeline primitives.

## Current Shape

```kotlin
data class AmplifyEffect(
    val countersPerReveal: Int,
    override val appliesTo: GameEvent = ZoneChangeEvent(...)
) : ReplacementEffect
```

Used by ~9 Legions cards (Kilnmouth Dragon, Embalmed Brawler, etc.).

## Target State

Decompose into a replacement effect that executes an atomic pipeline:

1. **Gather** cards from hand matching a filter (creatures sharing a type with entering creature)
2. **Select** any number to reveal (up to all)
3. **AddCounters** on entering creature: `count = revealed.size * countersPerReveal`

This could be expressed as:
```kotlin
replacementEffect {
    appliesTo = ZoneChangeEvent(filter = Creature, to = BATTLEFIELD)
    replacement = Effects.Composite(
        GatherCardsEffect(source = CardSource.Hand, filter = SharesTypeWithSelf, storeAs = "revealable"),
        SelectFromCollectionEffect(from = "revealable", mode = ChooseUpTo(all), label = "Reveal"),
        AddCountersEffect(target = Self, type = "+1/+1", count = Multiply(CollectionSize("selected"), countersPerReveal))
    )
}
```

This requires:
- A `SharesTypeWithSelf` filter predicate (or the entering creature's types passed through context)
- `CollectionSize(name)` as a `DynamicAmount` (may already exist as `VariableReference`)
- Pipeline effects working inside replacement effect context

## Complexity Note

The tricky part is that Amplify fires *during* entering the battlefield (as a replacement effect), not after. The pipeline needs to execute within the replacement effect resolution flow, and the counters need to be placed on the creature as it enters (not after ETB). This may require the pipeline to be integrated with the replacement effect system rather than the normal effect resolution.

## Files

- **SDK:** `ReplacementEffect.kt` (delete `AmplifyEffect`)
- **Engine:** replacement effect handlers
- **Sets:** `EmbalmedBrawler.kt`, `KilnmouthDragon.kt`, `CanopyCrawler.kt`, and ~6 other Legions cards
