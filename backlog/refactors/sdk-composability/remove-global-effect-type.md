# Remove GlobalEffectType Enum

## Problem

`GlobalEffectType` is a hardcoded enum where every value is already expressible by composing existing generic static abilities. Adding a new global enchantment effect (e.g., "all creatures have haste") requires adding an enum value + engine handler code, when it should be pure data.

## Mapping

Every `GlobalEffectType` value has a direct replacement using existing types:

| GlobalEffectType | Replacement |
|---|---|
| `ALL_CREATURES_GET_PLUS_ONE_PLUS_ONE` | `ModifyStatsForCreatureGroup(1, 1, GroupFilter.AllCreatures)` |
| `YOUR_CREATURES_GET_PLUS_ONE_PLUS_ONE` | `ModifyStatsForCreatureGroup(1, 1, GroupFilter.AllCreaturesYouControl)` |
| `OPPONENT_CREATURES_GET_MINUS_ONE_MINUS_ONE` | `ModifyStatsForCreatureGroup(-1, -1, GroupFilter.AllCreaturesOpponentsControl)` |
| `ALL_CREATURES_HAVE_FLYING` | `GrantKeywordToCreatureGroup(FLYING, GroupFilter.AllCreatures)` |
| `YOUR_CREATURES_HAVE_VIGILANCE` | `GrantKeywordToCreatureGroup(VIGILANCE, GroupFilter.AllCreaturesYouControl)` |
| `YOUR_CREATURES_HAVE_LIFELINK` | `GrantKeywordToCreatureGroup(LIFELINK, GroupFilter.AllCreaturesYouControl)` |
| `CREATURES_CANT_ATTACK` | `CantAttackForCreatureGroup(GroupFilter.AllCreatures)` — **new type needed** (or extend `CantAttack` to accept `GroupFilter`) |
| `CREATURES_CANT_BLOCK` | `CantBlockForCreatureGroup(GroupFilter.AllCreatures)` — already exists |
| `ALL_CREATURES_MUST_ATTACK` | `MustAttackForCreatureGroup(GroupFilter.AllCreatures)` — **new type needed** (or extend `MustAttack` to accept `GroupFilter`) |
| `ALL_CREATURES_MUST_BLOCK` | `MustBlockForCreatureGroup(GroupFilter.AllCreatures)` — **new type needed** |

## Approach

1. Add missing group-level combat static abilities: `CantAttackForCreatureGroup`, `MustAttackForCreatureGroup`, `MustBlockForCreatureGroup` (follow `CantBlockForCreatureGroup` as the pattern).
2. Wire them in `StaticAbilityHandler.convertStaticAbility()` — they should produce `ContinuousEffectData` with the appropriate `Modification` and `affectsFilter`, same as `convertGlobalEffect` does today.
3. Update all usages: `GrandMelee.kt`, `LayerSystemTest.kt`, `GrandMeleeTest.kt`.
4. Delete `GlobalEffect`, `GlobalEffectType`, and `convertGlobalEffect()`.
5. Update `reference.md`.

## Files

- **SDK:** `MiscStaticAbilities.kt` (delete `GlobalEffect` + `GlobalEffectType`), `CombatStaticAbilities.kt` (add new group types)
- **Engine:** `StaticAbilityHandler.kt` (delete `convertGlobalEffect`, add handlers for new types)
- **Sets:** `GrandMelee.kt`
- **Tests:** `LayerSystemTest.kt`, `GrandMeleeTest.kt`
