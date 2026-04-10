# Merge EntersWith*Choice Replacement Effects

## Problem

Three separate replacement effects for "as this enters, choose X":
- `EntersWithColorChoice`
- `EntersWithCreatureTypeChoice(opponentChooses)`
- `EntersWithCreatureChoice`

Each requires its own decision type, continuation, and component in the engine. A new choice type (e.g., "choose a card type", "choose a basic land type") would need another sealed class + engine wiring.

## Target State

Single parameterized type:

```kotlin
data class EntersWithChoice(
    val choiceType: ChoiceType,
    val chooser: Player = Player.You,   // replaces opponentChooses flag
    override val appliesTo: GameEvent = ...
) : ReplacementEffect

enum class ChoiceType {
    COLOR,
    CREATURE_TYPE,
    CREATURE_ON_BATTLEFIELD
    // future: BASIC_LAND_TYPE, CARD_TYPE, etc.
}
```

## Approach

1. Add `EntersWithChoice` and `ChoiceType` enum.
2. Update `StackResolver.kt` to dispatch on `choiceType` instead of matching three separate classes.
3. Migrate card definitions. Update `ModalAndCloneContinuations.kt`.
4. Delete the three old types.

The `ChoiceType` enum is still closed, but it's one enum shared across all enter-with-choice cards, rather than one sealed class per choice.

## Files

- **SDK:** `ReplacementEffect.kt`
- **Engine:** `StackResolver.kt`, `ModalAndCloneContinuations.kt`, `AffectsFilterResolver.kt`
- **Sets:** `SharedTriumph.kt`, `DoomCannon.kt`, `CoverOfDarkness.kt`, `DauntlessBodyguard.kt`, `RiptideReplicator.kt`, `PatchworkBanner.kt`, etc.
