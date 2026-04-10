# Compose Bespoke Stat Static Abilities from Generic Primitives

## Problem

Three static abilities in `StatsStaticAbilities.kt` encode specific P/T modification patterns that could be expressed by composing `ModifyStatsForCreatureGroup` or `GrantDynamicStatsEffect` with richer filters/amounts:

- **`ModifyStatsForChosenCreatureType(power, toughness, youControlOnly)`** — "+X/+X to creatures of the chosen type." Redundant with `ModifyStatsForCreatureGroup` if `GroupFilter` supported a `ChosenSubtype` predicate.
- **`ModifyStatsByCounterOnSource(counterType, powerMod, toughnessMod)`** — "+X/+X per counter on source." Redundant with `GrantDynamicStatsEffect` if `DynamicAmount.EntityProperty(Source, CounterCount(...))` were used.
- **`ModifyStatsPerSharedCreatureType(powerMod, toughnessMod)`** — "+X/+X per creature sharing a type." Redundant with `GrantDynamicStatsEffect` if `DynamicAmount.AggregateBattlefield` supported a `SharesTypeWith(AttachedCreature)` filter.

Each has its own handler branch in `StaticAbilityHandler` and `AffectsFilterResolver`.

## Target State

### ModifyStatsForChosenCreatureType
→ `ModifyStatsForCreatureGroup(power, toughness, GroupFilter.ChosenSubtypeCreatures(youControlOnly))`

This already almost works — `GroupFilter` has `ChosenSubtypeCreatures`. Check if it fully covers the `youControlOnly` flag. If so, this is a pure deletion.

### ModifyStatsByCounterOnSource
→ `GrantDynamicStatsEffect(target, powerBonus = Multiply(EntityProperty(Source, CounterCount(type)), perCounter), ...)`

Requires the `StateProjector` to evaluate `DynamicAmount` during projection, which it may not currently do for `GrantDynamicStatsEffect`.

### ModifyStatsPerSharedCreatureType
→ `GrantDynamicStatsEffect(target, powerBonus = Multiply(SharesTypeCount(AttachedCreature), perCreature), ...)`

Requires a `DynamicAmount` that counts creatures sharing a type with a referenced entity. This overlaps with the `CreaturesSharingTypeWithTriggeringEntity` refactor in `generic-dynamic-amount.md`.

## Approach

1. Check if `ModifyStatsForChosenCreatureType` can be replaced immediately with existing `ModifyStatsForCreatureGroup` + `GroupFilter.ChosenSubtypeCreatures`. If so, just delete it.
2. For the counter/shared-type variants, these depend on the `generic-dynamic-amount.md` refactor (Phase 2) landing first. Do those after.

## Files

- **SDK:** `StatsStaticAbilities.kt`
- **Engine:** `StaticAbilityHandler.kt`, `AffectsFilterResolver.kt`, `StateProjector.kt`
- **Sets:** `SharedTriumph.kt`, `PatchworkBanner.kt`, `WitheringHex.kt`, `AlphaStatus.kt`
