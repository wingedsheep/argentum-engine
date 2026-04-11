# Blight N (-1/-1 Counter Self-Distribution)

**Status:** Not implemented
**Cards affected in Lorwyn Eclipsed:** 23 mention Blight; 14 cards ship the reminder text (so ~14
distinct printings actually have it as a keyword action, the rest interact with -1/-1 counters
placed by it).
**Priority:** High — spans all five colors and shows up as an additional cost, activation cost,
and effect word.

## Rules text

> **Blight N** — Put N -1/-1 counters on a creature you control.

It's a keyword *action*, not a keyword ability. It appears in three grammatical positions in the
printed cards, and the implementation must support all three:

1. **Effect line:** "Blight 2. Draw a card." — Blight is just executed as part of the effect.
2. **Additional cost to cast:** "As an additional cost to cast this spell, you may blight 2."
   — optional additional cost; if paid, enables a bonus (e.g. `Pyrrhic Strike`'s "choose both").
3. **Activated ability cost:** "{1}{W}, Blight 1, Sacrifice this Aura: ..." — Blight is mixed
   into a cost line alongside mana and sacrifice.

Counter choice: the *active player's controller* picks which of their creatures receives the
counters, but it must be one of *their* creatures. Nothing in the rules prevents spreading across
multiple creatures in future printings, but every current ECL card says "a creature you control"
(singular), so we implement the singular-target form first.

## Implementation plan

### 1. SDK — add `Cost.Blight`
`mtg-sdk/.../scripting/cost/Cost.kt` (or wherever the `Cost` sealed interface lives)

```kotlin
@SerialName("Blight")
@Serializable
data class Blight(val amount: Int) : Cost {
    override val description = "Blight $amount"
}
```

Expose via `Costs.Blight(n)`. This handles use cases (2) and (3).

### 2. SDK — add `Effect.Blight`
For use case (1). Wire through `Effects.Blight(n)`:

```kotlin
@SerialName("Blight")
@Serializable
data class BlightEffect(val amount: DynamicAmount) : Effect {
    override val description = "Blight ${amount.description}"
}
```

Use `DynamicAmount` from the start so we can parameterize (e.g., "blight X" variants later).
Internally this is just a `PutCounters(kind = Minus1Minus1, amount = n, target = chooseCreatureYouControl)`
composite — implement it as a `CompositeEffect` in `EffectPatterns.kt` to stay consistent with
the atomic-effect guidance rather than adding a new executor:

```kotlin
// EffectPatterns.kt
fun blight(amount: DynamicAmount): Effect = CompositeEffect(listOf(
    ChooseTargetEffect(
        filter = GameObjectFilter.Creature.controlledByYou(),
        required = true,
        storeAs = "blightTarget"
    ),
    PutCountersOnStoredTargetEffect(
        storedKey = "blightTarget",
        counter = CounterType.Minus1Minus1,
        amount = amount
    )
))
```

If no `ChooseTargetEffect` / `PutCountersOnStoredTargetEffect` atoms exist yet, either add them or
map onto the closest existing primitives (the counters infra is already rich — see
`CounterEffects.kt`).

Edge case: if the controller has **no creatures**, blight does nothing but the cost/effect is
still paid/executed. Verify that the `ChooseTargetEffect` atom tolerates "no legal target" (CR
115.4) by skipping silently.

### 3. Engine — cost handler for `Cost.Blight`
In `rules-engine/.../mechanics/mana/` or wherever `Cost` handlers are aggregated:

- **Validation:** can pay if the player has at least one creature (or is the cost optional? — for
  additional-cost blight it's optional so always payable; for activation-cost blight it's
  mandatory and requires a creature target).
- **Payment:** at payment time, prompt the player to pick one of their creatures and mark the
  choice as part of the payment. When all costs are paid, apply the -1/-1 counters via the same
  executor used for `Effects.Blight`.
- **Continuation:** if the player has more than one legal creature, cost payment pauses for a
  `BlightTargetChoiceContinuation`. Reuse existing target-selection continuation patterns if
  possible rather than adding a new frame type.

### 4. Engine — "if the additional cost was paid" condition
Cards like `Pyrrhic Strike` check whether the optional blight cost was paid
("If this spell's additional cost was paid, choose both instead"). The engine must:
- Track paid optional additional costs on the stack object when the spell is cast.
- Expose a `Conditions.PaidAdditionalCost(kind = Blight)` condition that resolvers can branch on.

There is almost certainly existing infra for this — Kicker works the same way. Check how
`Conditions.WasKicked` is built and model Blight's condition identically.

### 5. Card DSL

```kotlin
// Spiral into Solitude — activated ability cost
card("Spiral into Solitude") {
    manaCost = "{1}{W}"
    typeLine = "Enchantment — Aura"
    // ... aura targeting ...
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{W}"),
            Costs.Blight(1),
            Costs.SacrificeSelf
        )
        effect = Effects.ExileEnchanted
    }
}

// Pyrrhic Strike — optional additional cost
card("Pyrrhic Strike") {
    manaCost = "{2}{W}"
    typeLine = "Instant"
    additionalCost(Costs.Blight(2), optional = true)
    spell {
        effect = Effects.ModalChoice(
            chooseOneOrBothIf = Conditions.PaidAdditionalCost(Cost.Blight::class),
            Effects.Destroy(TargetFilter.Artifact.or(TargetFilter.Enchantment)),
            Effects.Destroy(TargetFilter.Creature.withManaValue(AtLeast(3)))
        )
    }
}
```

### 6. Tests

- Unit: `Costs.Blight(2)` requires a creature you control to pay; if you have none, the cost is
  illegal (for activations) or silently skipped (for the optional additional-cost form).
- Scenario: cast `Pyrrhic Strike` without paying blight → only one mode chosen. Cast it while
  paying blight → both modes, and one of your creatures has 2 -1/-1 counters.
- Scenario: activate `Spiral into Solitude` — creature loses a toughness and aura exiles target.
- Scenario: `Evershrike's Gift` reanimation activates from graveyard using blight cost.
- Interaction: SBAs remove a creature that goes to 0 toughness from blight payment *before* the
  effect resolves (cost payment happens before effect resolution → SBAs check → then effect).
  Especially verify `Pyrrhic Strike`'s "destroy target creature w/ mv 3+" still has a legal target
  after SBAs if the blighted creature was also the intended target.
- Interaction: blight on a creature that has a +1/+1 counter cancels one of each via SBA (rule 704).

## Dependencies

- No new infrastructure. Everything needed — counter placement, cost framework, additional-cost
  tracking, condition evaluation — already exists.
- `Wither` (separate doc) is a sibling mechanic but not a dependency.
