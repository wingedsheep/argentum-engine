# Mana-Spent-to-Cast Gated ETB Triggers

**Status:** Not implemented
**Cards affected in Lorwyn Eclipsed:** 5 — the full Incarnation cycle (`Catharsis`, `Deceit`,
`Emptiness`, `Wistfulness`, one more). Each has two ETB triggers, each gated on a specific
colored-mana payment.
**Priority:** High — paired with `evoke.md`; these are the same five cards.

## Rules text

> When this creature enters, **if {W}{W} was spent to cast it**, create two 1/1 Kithkin creature
> tokens.

This is Lorwyn's "Incarnation" pattern. The trigger condition looks at the *actual mana spent* to
cast the spell, not the mana cost on the card, not the converted mana value. A card with mana cost
`{4}{R/W}{R/W}` can be paid as `{4}{R}{R}`, `{4}{W}{W}`, or `{4}{R}{W}`. Only the first fires the
red ETB, only the second fires the white ETB, both trigger if both colors were paid — and Evoke
(`{R/W}{R/W}` = two hybrid mana) can also trigger either branch depending on how it's paid.

Key rule details:
- "Was spent" reads the **history** of that specific cast, so the check must happen against a
  recorded cast event, not a live mana pool.
- Cost reductions don't matter — what was *actually* paid is what's counted.
- If the spell is copied (not cast), no mana was spent, so neither gate fires. Same for "put onto
  the battlefield" effects.
- Multiple independent gates on the same card are evaluated independently; both can be true.

## Implementation plan

### 1. Engine — record mana spent on cast
There's already a `ManaSpentEvent` (found during exploration of `TypecycleCardHandler.kt`). Audit
whether the cast pipeline aggregates per-spell mana spent and stores it on the resulting stack
object. If not:

- Extend `SpellOnStack` / `StackObject` (wherever spells live on the stack) with a
  `manaSpent: ManaCost` field.
- During `CastPaymentProcessor`, accumulate all mana payments into that field and stamp it onto
  the stack object at the end of cost payment.
- When the spell resolves and a permanent enters, propagate the record onto the resulting
  permanent as a transient `CastRecordComponent(manaSpent: ManaCost)`. Strip on zone change, like
  other battlefield-only components.

### 2. SDK — `Condition.ManaSpentToCastIncludes`
`mtg-sdk/.../scripting/conditions/GenericConditions.kt`

```kotlin
@SerialName("ManaSpentToCastIncludes")
@Serializable
data class ManaSpentToCastIncludes(
    val required: ManaCost
) : Condition {
    override val description = "${required} was spent to cast this spell"
}
```

"Includes" means: for each colored pip in `required`, the recorded `manaSpent` has at least that
many of the same color. Generic mana pips in `required` check only total count, not color.

Wire through `Conditions.ManaSpentToCastIncludes("{W}{W}")`.

### 3. Engine — evaluator
In the condition evaluator registry (grep for how `Conditions.Void`-style turn conditions are
evaluated), add:

```kotlin
is ManaSpentToCastIncludes -> {
    val selfId = context.sourceEntity ?: return false
    val record = state.getEntity(selfId)
        .get<CastRecordComponent>()?.manaSpent
        ?: return false
    record.coversPips(condition.required)
}
```

`coversPips` should be a small helper on `ManaCost` that returns true when `this` has at least
`required.white` white pips, `required.blue` blue pips, etc.

### 4. Trigger wiring
For triggered abilities, the condition goes in `triggerCondition`. `TriggerMatcher` already
re-checks trigger conditions at the "intervening if" time per CR 603.4, so this should work
out of the box once the condition evaluator is in place.

### 5. Edge cases to cover in tests

- `{4}{W}{W}` paid conventionally → white gate fires, red gate does not.
- Hybrid card `{R/W}{R/W}` paid as `{R}{W}` → neither {W}{W} nor {R}{R} gate fires.
- Paid via Evoke `{R/W}{R/W}` = `{W}{W}` → white gate fires, creature is sacrificed on entry
  (see `evoke.md`) but its ETB still resolves first.
- Spell copied via Twinflame → no mana spent, no gate fires.
- Spell cast for alternative cost that doesn't include the gated color → no trigger.
- Spell cast with Convoke contributing {W} → mana-spent tally includes {W} (convoke taps count
  as spending mana of the tapped creature's color, CR 702.51).
- Reanimated (not cast) onto the battlefield → no `CastRecordComponent`, condition is false.

## Dependencies

- Build on existing `ManaSpentEvent` instrumentation if any.
- No dependency on Evoke; the two features are complementary but orthogonal. Implement this one
  first — it's testable without Evoke.
