# Phase 11 — DSL Convenience Functions + Deprecate/Delete Old Zone Effects

## Goal

Add ergonomic DSL convenience functions so card authors don't need to write `MoveToZoneEffect(...)` directly. Then deprecate and delete the six old effect types and their executors.

## Steps

### 11a — Add DSL convenience functions

**File:** `mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/dsl/Effects.kt` (or existing facade)

```kotlin
object Effects {
    fun Destroy(target: EffectTarget) =
        MoveToZoneEffect(target, Zone.Graveyard, byDestruction = true)

    fun Exile(target: EffectTarget) =
        MoveToZoneEffect(target, Zone.Exile)

    fun ReturnToHand(target: EffectTarget) =
        MoveToZoneEffect(target, Zone.Hand)

    fun PutOnTopOfLibrary(target: EffectTarget) =
        MoveToZoneEffect(target, Zone.Library, ZonePlacement.Top)

    fun ShuffleIntoLibrary(target: EffectTarget) =
        MoveToZoneEffect(target, Zone.Library, ZonePlacement.Shuffled)

    fun PutOntoBattlefield(target: EffectTarget, tapped: Boolean = false) =
        MoveToZoneEffect(target, Zone.Battlefield,
            if (tapped) ZonePlacement.Tapped else ZonePlacement.Default)
}
```

Card authors can then write `Effects.Destroy(EffectTarget.ContextTarget(0))` instead of `MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.Graveyard, byDestruction = true)`.

### 11b — Deprecate old effect types

Add `@Deprecated` with `ReplaceWith` to each old effect:

| Effect | ReplaceWith |
|--------|------------|
| `DestroyEffect` | `MoveToZoneEffect(target, Zone.Graveyard, byDestruction = true)` |
| `ReturnToHandEffect` | `MoveToZoneEffect(target, Zone.Hand)` |
| `ReturnFromGraveyardEffect` | `MoveToZoneEffect(target, destination)` |
| `ExileEffect` | `MoveToZoneEffect(target, Zone.Exile)` |
| `ShuffleIntoLibraryEffect` | `MoveToZoneEffect(target, Zone.Library, ZonePlacement.Shuffled)` |
| `PutOnTopOfLibraryEffect` | `MoveToZoneEffect(target, Zone.Library, ZonePlacement.Top)` |

### 11c — Deprecate `TargetCardInGraveyard`

```kotlin
@Deprecated("Use TargetObject with appropriate TargetFilter",
    ReplaceWith("TargetObject(filter = filter)"))
data class TargetCardInGraveyard(...) : TargetRequirement
```

### 11d — Delete deprecated types and executors

After verifying no references remain:

**SDK deletions:**
- `DestroyEffect` (from `RemovalEffects.kt`)
- `ReturnToHandEffect` (from `RemovalEffects.kt`)
- `ReturnFromGraveyardEffect` (from `RemovalEffects.kt`)
- `ExileEffect` (from `RemovalEffects.kt`)
- `ShuffleIntoLibraryEffect` (from `LibraryEffects.kt`)
- `PutOnTopOfLibraryEffect` (from `LibraryEffects.kt`)
- `TargetCardInGraveyard` (from `TargetRequirement.kt`)
- `SearchDestination` enum (if no longer used by other effects)

**Engine executor deletions:**
- `DestroyEffectExecutor`
- `ReturnToHandEffectExecutor`
- `ReturnFromGraveyardEffectExecutor`
- `ExileEffectExecutor`
- `ShuffleIntoLibraryEffectExecutor`
- `PutOnTopOfLibraryEffectExecutor`

Remove executor registrations from `EffectExecutorRegistry.kt`.

### 11e — Full test suite

Run all tests to verify clean deletion:
```bash
just test
```

## Risk

Low — all card migrations are already done in Phase 10. This phase is purely annotation + deletion.
