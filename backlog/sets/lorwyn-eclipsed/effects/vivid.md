# Vivid (Colors-Among-Permanents Scaling)

**Status:** Partially implemented — cost-reduction half exists as
`CostReductionSource.ColorsAmongPermanentsYouControl`. The *effect-scaling* half does not.
**Cards affected in Lorwyn Eclipsed:** 14.
**Priority:** High — second most common new mechanic, sprinkled across all five colors.

## Rules text

Vivid is an **ability word** (no rules text of its own; just a flavor prefix). It appears in two
concrete shapes:

1. **Cost reduction:** "Vivid — This spell costs {1} less to cast for each color among permanents
   you control." (e.g., `Rime Chill`)
2. **Scaled effect:** "Vivid — When this creature enters, do X N times, where N is the number of
   colors among permanents you control." (e.g., `Kithkeeper`, `Shinestriker`)

Counting rule: look at permanents you control, union their color sets (colorless contributes
nothing), count the size of that set. Max is 5. Projected colors count — if Mistform Ultimus
becomes blue-only via a type/color effect, it counts as blue. Always evaluate via
`projectedState.getColors(entityId)`, not base card colors, per the project's battlefield-filter
rule.

## Implementation plan

### Part A — effect-scaling half (the new work)

#### 1. SDK — new `DynamicAmount` case
`mtg-sdk/.../scripting/values/DynamicAmount.kt`

```kotlin
@SerialName("ColorsAmongPermanents")
@Serializable
data class ColorsAmongPermanents(
    val controller: Player = Player.You,
    val filter: GameObjectFilter = GameObjectFilter.Permanent
) : DynamicAmount {
    override val description: String =
        "the number of colors among ${filter.description} ${controller.possessive} control"
    override fun applyTextReplacement(replacer: TextReplacer) = this
}
```

Parameterizing `controller` + `filter` costs nothing now and lets us reuse the value for future
"colors among creatures you control" / "colors among permanents target opponent controls" text.
Default the filter to `Permanent` for ECL.

#### 2. Engine — evaluator
In the `DynamicAmount` resolver (search for the `when` branch that handles the existing
`AggregateBattlefield`):

```kotlin
is DynamicAmount.ColorsAmongPermanents -> {
    val controllerIds = resolvePlayerRef(amount.controller, context)
    val projected = context.projectedState
    val permanents = state.getBattlefield()
        .filter { projected.getController(it) in controllerIds }
        .filter { projected.matchesFilter(it, amount.filter, state) }
    permanents
        .flatMap { projected.getColors(it) }  // projected, not base
        .toSet()
        .size
}
```

Must use `projectedState.getColors(entityId)` — base colors are insufficient if a continuous
effect has recolored a permanent.

#### 3. Card DSL example (Kithkeeper)

```kotlin
card("Kithkeeper") {
    manaCost = "{6}{W}"
    typeLine = "Creature — Elemental"
    power = 5; toughness = 5
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateTokens(
            count = Values.ColorsAmongPermanents(),
            token = TokenPatterns.Kithkin
        )
    }
    activatedAbility { /* Tap three creatures ... */ }
}
```

### Part B — cost-reduction half (already done)

`CostReductionSource.ColorsAmongPermanentsYouControl` already exists and is plumbed through the
cost system, so cards like `Rime Chill` can use the existing
`SpellCostReduction(ColorsAmongPermanentsYouControl)` static ability on a card's self. Verify it
resolves correctly against the projected state (same caveat as Part A — colors must be read from
`ProjectedState`).

If the existing implementation reads base `CardComponent.colors`, fix it before wiring ECL cards.
Search: `ColorsAmongPermanentsYouControl` in `rules-engine/` and audit the evaluator.

### 4. Tests

- Unit: `DynamicAmount.ColorsAmongPermanents` evaluates to the correct count for a mix of mono-,
  multi-, and colorless permanents.
- Unit: recolor effect (e.g., Chromatic Lantern–style) changes the count — base-state value must
  be ignored.
- Scenario: `Kithkeeper` ETBs with 3 colors on board → creates 3 Kithkin tokens.
- Scenario: `Shinestriker` ETBs with 5 colors on board → draws 5 cards.
- Scenario: `Rime Chill` costs reduced correctly, including when colors change post-announcement
  (per CR, re-check at actual payment time).
- Regression: colorless permanents (Wastes, eldrazi, artifacts) do not contribute to the count.

## Dependencies

- None. Both pieces are additive.
- Reuse of the new `DynamicAmount.ColorsAmongPermanents` for the cost-reduction side is a nice
  cleanup but out of scope for ECL card implementation — keep the two code paths until the set
  lands, then refactor.

## Cross-reference

Already on the existing backlog under a slightly different name:
[sdk-composability-gaps.md §4c "NumberOfColorsAmong DynamicAmount"](../../../sdk-composability-gaps.md#4c-numberofcolorsamongfilter-gameobjectfilter).
That entry is currently Priority 3. Lorwyn Eclipsed promotes it to Priority 1. When picking the
name, align with whatever §4c eventually lands on — don't ship two competing `DynamicAmount`
cases.
