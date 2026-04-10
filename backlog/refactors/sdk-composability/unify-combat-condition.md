# Replace CombatCondition with Condition

## Problem

`CombatCondition` is a separate sealed interface with only 2 variants (`ControlMoreCreatures`, `OpponentControlsLandType`), used by `CantAttackUnless` and `CantBlockUnless`. Every new "can't attack/block unless" pattern requires a new variant. The existing `Condition` system already handles arbitrary game-state queries.

## Target State

Replace `CantAttackUnless(condition: CombatCondition)` with `CantAttackUnless(condition: Condition)` and delete `CombatCondition`.

The two existing conditions map to:
- `ControlMoreCreatures` → `Condition.Compare(Count(You, BATTLEFIELD, Creature), ">", Count(Opponent, BATTLEFIELD, Creature))`
- `OpponentControlsLandType("Island")` → `Condition.Exists(Player.Opponent, BATTLEFIELD, GameObjectFilter.Land.withSubtype("Island"))`

## Files

- **SDK:** `CombatStaticAbilities.kt` (delete `CombatCondition`, change field type)
- **Engine:** `AttackRestrictionRules.kt`, `BlockPhaseManager.kt` (replace `evaluateCombatCondition()` with `ConditionEvaluator`)
- **Sets:** `GoblinGoon.kt`, `SlipstreamEel.kt`, `DeepSeaSerpent.kt`
