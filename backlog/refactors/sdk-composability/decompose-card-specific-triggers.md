# Decompose Card-Specific Trigger Events

## Problem

Several `GameEvent` variants encode narrow scenarios instead of composing from generic event + condition:

- **`CreatureDealtDamageBySourceDiesEvent`** — "creature dealt damage by this creature this turn dies." Should be a death trigger with a condition. Currently unused by any card.
- **`AttackEvent.alone`** — boolean flag for "attacks alone." Should be a trigger condition, not a flag on the event. Currently unused by any card.

## Target State

### CreatureDealtDamageBySourceDiesEvent

Replace with a death trigger + `triggerCondition`:
```kotlin
triggeredAbility {
    trigger = Triggers.CreatureDies
    triggerCondition = Conditions.TriggeringEntityWasDealtDamageBySource
    effect = ...
}
```

Requires adding `Conditions.TriggeringEntityWasDealtDamageBySource` (the engine already tracks `DamageDealtToCreaturesThisTurnComponent`).

### AttackEvent.alone

Replace with a trigger condition on the standard `AttackEvent`:
```kotlin
triggeredAbility {
    trigger = Triggers.Attacks
    triggerCondition = Conditions.SourceAttacksAlone
    effect = ...
}
```

Or check attacker count: `Conditions.Compare(Count(You, attackers), "==", Fixed(1))`.

## Approach

1. Add the new conditions to the `Condition` system.
2. Update `Triggers.kt` facades (`Triggers.AttacksAlone`, `Triggers.CreatureDealtDamageBySourceDies`).
3. Remove `alone` field from `AttackEvent` and delete `CreatureDealtDamageBySourceDiesEvent`.
4. Update `TriggerMatcher.kt` to remove special handling.

## Files

- **SDK:** `GameEvent.kt`, `Triggers.kt`, condition files
- **Engine:** `TriggerMatcher.kt`, `DeathAndLeaveTriggerDetector.kt`
- **Sets:** None currently use these
