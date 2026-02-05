# Phase 9 — Add `MoveToZoneEffect`, `ZonePlacement`, and `TargetObject`

## Goal

Add the foundation types for the unified zone-moving effect: `MoveToZoneEffect`, `ZonePlacement` enum, and `TargetObject` target requirement. Create the engine executor and register it.

## Steps

### 9a — Add `ZonePlacement` enum

**File:** `mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/scripting/effects/ZonePlacement.kt` (new)

```kotlin
enum class ZonePlacement {
    Default,
    Top,
    Bottom,
    Shuffled,
    Tapped
}
```

### 9b — Add `MoveToZoneEffect`

**File:** `mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/scripting/effects/RemovalEffects.kt`

```kotlin
@Serializable
data class MoveToZoneEffect(
    val target: EffectTarget,
    val destination: Zone,
    val placement: ZonePlacement = ZonePlacement.Default,
    val byDestruction: Boolean = false
) : Effect {
    override val description: String = buildString {
        when {
            byDestruction -> append("Destroy ${target.description}")
            destination == Zone.Hand -> append("Return ${target.description} to its owner's hand")
            destination == Zone.Exile -> append("Exile ${target.description}")
            destination == Zone.Library && placement == ZonePlacement.Shuffled ->
                append("Shuffle ${target.description} into its owner's library")
            destination == Zone.Library && placement == ZonePlacement.Top ->
                append("Put ${target.description} on top of its owner's library")
            destination == Zone.Battlefield && placement == ZonePlacement.Tapped ->
                append("Put ${target.description} onto the battlefield tapped")
            destination == Zone.Battlefield ->
                append("Put ${target.description} onto the battlefield")
            else -> append("Put ${target.description} into ${destination.description}")
        }
    }
}
```

Uses the existing `Zone` enum from `com.wingedsheep.sdk.scripting.events.Zone`.

### 9c — Add `TargetObject` to TargetRequirement

**File:** `mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/targeting/TargetRequirement.kt`

```kotlin
@Serializable
data class TargetObject(
    override val count: Int = 1,
    override val optional: Boolean = false,
    val filter: TargetFilter
) : TargetRequirement {
    override val description: String = buildString {
        if (optional) append("up to ")
        append("target ")
        append(filter.description)
    }
}
```

This generalizes `TargetCardInGraveyard` — the `TargetFilter` carries zone information (e.g., `TargetFilter.CreatureInYourGraveyard` has `zone = Zone.Graveyard`).

### 9d — Create `MoveToZoneEffectExecutor`

**File:** `rules-engine/src/main/kotlin/com/wingedsheep/engine/handlers/effects/removal/MoveToZoneEffectExecutor.kt` (new)

The executor must handle:
- **Default move:** Resolve target entity, find its current zone, move to destination zone
- **byDestruction = true:** Check indestructible, apply regeneration replacement effects, emit `DestroyedEvent`
- **Placement:** `Top`/`Bottom` for library, `Tapped` for battlefield, `Shuffled` for library + shuffle
- **Zone-appropriate events:** `DiesEvent` (graveyard from battlefield via destruction), `ExiledEvent`, etc.

Consolidates logic from existing executors:
- `DestroyEffectExecutor`
- `ExileEffectExecutor`
- `ReturnToHandEffectExecutor`
- `ReturnFromGraveyardEffectExecutor`
- `ShuffleIntoLibraryEffectExecutor`
- `PutOnTopOfLibraryEffectExecutor`

### 9e — Register in `EffectExecutorRegistry`

**File:** `rules-engine/src/main/kotlin/com/wingedsheep/engine/handlers/effects/EffectExecutorRegistry.kt`

Add `MoveToZoneEffect::class to MoveToZoneEffectExecutor()`.

### 9f — Add `TargetObject` validation in `TargetValidator`

**File:** `rules-engine/src/main/kotlin/com/wingedsheep/engine/mechanics/targeting/TargetValidator.kt`

Add a `TargetObject` case that evaluates the `TargetFilter` against the game state. Since `TargetFilter` already carries zone info, the validator checks:
1. Target exists in the specified zone
2. Target matches the filter's `baseFilter`
3. Target belongs to the correct player (if filter specifies ownership)

## Testing

- Unit test: `MoveToZoneEffectExecutor` with each destination + placement combination
- Verify `TargetObject` validation accepts/rejects correct targets
- Ensure old executors still work (no breaking changes — this phase is purely additive)

## Risk

Medium — new effect type and executor, but purely additive. No existing code changes.
