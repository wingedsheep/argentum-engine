# Unify Blocking Evasion Static Abilities

## Problem

There are 12+ separate `StaticAbility` sealed class variants in `BlockingStaticAbilities.kt` for blocking restrictions, each with a corresponding `BlockEvasionRule` class in the engine. Every new blocking pattern (e.g., "can't be blocked by artifact creatures") requires adding a new SDK class + a new engine rule class. These are all variations on two concepts:

1. **"Can't be blocked by X"** — blocker must NOT match a filter
2. **"Can't be blocked except by X"** — blocker MUST match a filter

## Current Variants

**"Can't be blocked by X" family:**
- `CantBeBlockedByColor(colors)` — blocker has color
- `CantBeBlockedByPower(minPower)` — blocker power >= N
- `CantBeBlockedByPowerOrLess(maxPower)` — blocker power <= N
- `CantBeBlockedBySubtype(subtype)` — blocker has subtype
- `CantBeBlockedByMoreThan(maxBlockers)` — limits blocker count (different axis, keep separate)

**"Can't be blocked except by X" family:**
- `CantBeBlockedExceptByKeyword(keyword)` — blocker needs keyword
- `GrantCantBeBlockedExceptBySubtype(filter, subtype)` — group evasion, blocker needs subtype

**Conditional unblockable:**
- `CantBeBlockedIfCastSpellType(spellType)` — conditional on turn state (different axis, keep separate for now)
- `GrantCantBeBlockedToSmallCreatures(maxValue)` — group grant based on attacker stats (different axis)
- `CantBeBlockedUnlessDefenderSharesCreatureType(minCount)` — condition on defending player's board (different axis)

**Blocker-side restrictions:**
- `CanOnlyBlockCreaturesWithKeyword(keyword)` — restricts what THIS creature can block
- `CantBlockCreaturesWithGreaterPower` — restricts what THIS creature can block

## Target State

Collapse the "can't be blocked by X" and "can't be blocked except by X" families into two parameterized types that take a `GameObjectFilter` for the blocker:

```kotlin
// "Can't be blocked by creatures matching filter"
// Replaces: CantBeBlockedByColor, CantBeBlockedByPower, CantBeBlockedByPowerOrLess, CantBeBlockedBySubtype
data class CantBeBlockedBy(
    val blockerFilter: GameObjectFilter,
    val target: StaticTarget = StaticTarget.SourceCreature
) : StaticAbility

// "Can't be blocked except by creatures matching filter"
// Replaces: CantBeBlockedExceptByKeyword
data class CantBeBlockedExceptBy(
    val blockerFilter: GameObjectFilter,
    val target: StaticTarget = StaticTarget.SourceCreature
) : StaticAbility
```

Usage examples:
```kotlin
// Sacred Knight: can't be blocked by black and/or red creatures
CantBeBlockedBy(GameObjectFilter.Creature.withAnyColor(BLACK, RED))

// Juggernaut: can't be blocked by Walls
CantBeBlockedBy(GameObjectFilter.Creature.withSubtype("Wall"))

// Fleet-Footed Monk: can't be blocked by creatures with power 2 or greater
CantBeBlockedBy(GameObjectFilter.Creature.powerAtLeast(2))

// War-Name Aspirant: can't be blocked by creatures with power 1 or less
CantBeBlockedBy(GameObjectFilter.Creature.powerAtMost(1))

// CantBeBlockedExceptByKeyword(FLYING) becomes:
CantBeBlockedExceptBy(GameObjectFilter.Creature.withKeyword(FLYING))
```

## What to Keep Separate

These don't fit the simple "filter the blocker" pattern and should remain as-is for now:
- `CantBeBlockedByMoreThan(maxBlockers)` — counts blockers, not a filter on individual blockers
- `CantBeBlockedIfCastSpellType` — conditional on turn history, not on the blocker
- `GrantCantBeBlockedToSmallCreatures` — scans controller's board for the ability source, filters attacker not blocker
- `CantBeBlockedUnlessDefenderSharesCreatureType` — condition on defending player's board state
- `CanOnlyBlockCreaturesWithKeyword` / `CantBlockCreaturesWithGreaterPower` — blocker-side restrictions (restrict what the blocker can block, not what the attacker can be blocked by)

## Approach

1. Add `CantBeBlockedBy` and `CantBeBlockedExceptBy` to the SDK. Ensure `GameObjectFilter` has the needed predicates (`withAnyColor`, `powerAtLeast`, `powerAtMost` — check if these exist).

2. In the engine, replace the 4-5 individual `BlockEvasionRule` classes with one `CantBeBlockedByRule` and one `CantBeBlockedExceptByRule` that evaluate the blocker against the `GameObjectFilter` using projected state.

3. Migrate card definitions and tests.

4. Delete the old types and rules. Also delete `GrantCantBeBlockedExceptBySubtype` — it can become `CantBeBlockedExceptBy` with appropriate filter + group grant mechanism (or a group-level variant `GrantBlockingEvasionToGroup(evasion, filter)`).

## Files

- **SDK:** `BlockingStaticAbilities.kt` (replace 5+ classes with 2)
- **Engine:** `BlockEvasionRules.kt` (replace 5+ rule classes with 2), `StaticAbilityHandler.kt` (if relevant)
- **Sets:** `SacredKnight.kt`, any other cards using the old types
- **Tests:** blocking-related scenario tests
