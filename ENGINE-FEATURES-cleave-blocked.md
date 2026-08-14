# Engine features surfaced by the VOW Cleave batch

While implementing the Innistrad: Crimson Vow (VOW) Cleave cards, **10 of 12** cleave cards are now
authored (5 pre-existing reference cards — Alchemist's Gambit, Dig Up, Fierce Retribution, Path of
Peril, Wash Away — plus this batch's Alchemist's Retrieval, Dread Fugue, Lunar Rejection, Parasitic
Grasp, Winged Portent).

The **Cleave keyword itself is fully implemented** (`AlternativeCostType.CLEAVE` + the
`cleaveTarget` / `cleaveEffect` DSL, modeled as a cast-mode branch rather than string mutation — see
[`docs/card-sdk-language-reference.md`](docs/card-sdk-language-reference.md)). The two remaining cards
are **not** blocked by cleave. Each needs an *orthogonal* engine capability that cleave happens to be
the first VOW card to require. This document specifies both so they can be scoped as `add-feature`
work.

Both are `add-feature` territory (new SDK type + engine wiring across layers), not `add-card`
authoring. Neither card should be shipped as a lossy approximation.

---

## 1. Inspired Idea — reduce maximum hand size by N for the rest of the game

**Card** (VOW #139, `{2}{U}` Sorcery, Cleave `{3}{U}{U}`):

> Draw three cards. **[Your maximum hand size is reduced by three for the rest of the game.]**
>
> *Cleave `{3}{U}{U}`* — paying the cleave cost removes the bracketed sentence, so the cleaved cast
> is just "Draw three cards."

The cleave half is trivial (`effect` = draw 3 + reduce; `cleaveEffect` = draw 3). The blocker is the
bracketed clause: **a one-shot resolution effect that permanently reduces the caster's maximum hand
size by a delta, for the rest of the game.**

### Why the existing hand-size machinery doesn't cover it

`MaximumHandSize.effective()`
(`rules-engine/src/main/kotlin/com/wingedsheep/engine/core/MaximumHandSize.kt`) is the single source
of truth read by both the cleanup step (`CleanupPhaseManager`) and the client view
(`ClientStateTransformer`). It starts from `DEFAULT = 7` (CR 402.2) and folds in every
`SetMaximumHandSize` **static ability on a battlefield permanent**, taking the most restrictive value.

That model is a poor fit for Inspired Idea on three counts:

1. **It's an absolute set, not a relative delta.** `SetMaximumHandSize(amount)` sets the ceiling to
   `amount`. Inspired Idea *subtracts* — and stacks: cast it twice and the max drops by six. There is
   no accumulating "reduce by N" primitive, and you can't express one as a `SetMaximumHandSize`
   because a later reduction has to compose with an earlier one.
2. **It's a static ability sourced from a permanent, not a one-shot from a resolving spell.**
   `MaximumHandSize.effective()` only scans the battlefield for static abilities. A resolved sorcery
   leaves nothing on the battlefield; the reduction has to live as **player state**, not a permanent's
   ability.
3. **"For the rest of the game" needs a durable per-player component.** The only existing player-state
   analog is `PlayerNoMaximumHandSizeComponent` (Wisdom of Ages — a boolean "no maximum"). There is no
   component that carries an integer reduction that survives to end of game.

### Suggested shape (general, not card-specific)

- **New per-player component** `PlayerMaximumHandSizeReductionComponent(amount: Int)` (accumulating —
  a second application adds to `amount`), stored on the player entity. Register it in
  `Serialization.kt` alongside `PlayerNoMaximumHandSizeComponent`.
- **New one-shot effect** `ReduceMaximumHandSizeEffect(target: EffectTarget = Controller, amount:
  DynamicAmount)` in `mtg-sdk/.../scripting/effects/PlayerEffects.kt`, facade
  `Effects.ReduceMaximumHandSize(amount)` in `Effects.kt`. Parameterize `target` and `amount` so it
  isn't Inspired-Idea-specific (any player, any delta). The executor adds/accumulates the component.
- **Wire the read into `MaximumHandSize.effective()`**: after computing the static-ability minimum,
  subtract the target player's `PlayerMaximumHandSizeReductionComponent.amount` (clamp at ≥ 0). Because
  both cleanup and the client badge already route through `effective()`, this single wiring point keeps
  the enforced number and the displayed number in agreement — the invariant that file's doc comment
  calls out.
- **Interaction with "no maximum":** `hasNoMaximum()` must still win outright (Reliquary Tower over a
  prior reduction → no discard).

### Test surface

- Cast printed Inspired Idea → draw 3, max hand size shows 4, cleanup discards down to 4.
- Cast it twice → max 1.
- Cleaved cast → draw 3, max stays 7.
- Reduction + a Reliquary Tower on the battlefield → no discard (no-maximum wins).

---

## 2. Lantern Flare — `{X}` inside the Cleave cost

**Card** (VOW #153, `{1}{W}` Instant, Cleave `{X}{R}{W}`):

> Lantern Flare deals X damage to target creature or planeswalker and you gain X life. **[X is the
> number of creatures you control.]**
>
> *Cleave `{X}{R}{W}`* — paying the cleave cost removes the bracketed sentence. Now the `{X}` in the
> cleave cost is a **player-chosen paid value**, and that chosen X is what the effect uses.

This card is the mirror-image of a normal X spell:

- **Printed mode** (`{1}{W}`, no `{X}` in cost): X is a *defined dynamic amount* — "the number of
  creatures you control." The player pays no X; the effect reads the board. This half is authorable
  today (`DynamicAmounts.creaturesYouControl()`).
- **Cleaved mode** (`{X}{R}{W}`): the sentence defining X is removed, so `{X}` in the **cleave cost**
  becomes the source of X. The player chooses X at cast time, pays for it, and the effect deals/gains
  that chosen X.

### Why it's blocked

`enumerateCleave` (`CastSpellEnumerator.kt`, ~line 2053) computes the cleave cost and its
affordability, but — unlike the normal-cast path — it **never computes `hasXCost` / `maxAffordableX`,
and never surfaces the X choice to the client.** The normal path does this at
`CastSpellEnumerator.kt:661`:

```kotlin
val hasXCost = effectiveCost.hasX
val maxAffordableX: Int? = if (hasXCost) {
    val availableSources = context.manaSolver.getAvailableManaCount(...)
    val fixedCost = effectiveCost.cmc            // X contributes 0 to CMC
    val xSymbolCount = effectiveCost.xCount.coerceAtLeast(1)
    ((availableSources - fixedCost) / xSymbolCount).coerceAtLeast(0)
} else null
```

The cleave `LegalAction`s emitted by `enumerateCleave` carry no `hasXCost` / `maxAffordableX`, so the
client can't prompt for X and the chosen value can't flow through
`CastSpell.xValue → SpellOnStackComponent.xValue → DynamicAmount.XValue`. Cleave is the first
alternative cost in the codebase that can itself contain `{X}` (kicker-with-X folds X into the printed
cost, a different path).

### Suggested shape

- In `enumerateCleave`, after computing `cleaveCost`, compute `hasXCost = cleaveCost.hasX` and
  `maxAffordableX` exactly as the normal path does (reuse the same solver call), and thread both into
  every cleave `LegalAction` (all three emission branches: auto-select target, targeted, no-target).
- Verify the cast handler already binds `CastSpell.xValue` for the alternative-cost path so the
  resolving effect's `DynamicAmount.XValue` reads the chosen X. If the cleave resolution path drops
  `xValue`, wire it through (mirror the "propagate `targets`" continuation rule in
  `rules-engine/CLAUDE.md`).
- **Faithfulness note:** the printed effect's `DynamicAmount` must be `XValue` (chosen X) in cleaved
  mode but `creaturesYouControl()` in printed mode. Cleave already supports a distinct `cleaveEffect`,
  so this is expressible once the enumerator offers X on the cleave action — the two modes bind X from
  different sources.

### Test surface

- Printed Lantern Flare with 3 creatures out → 3 damage / gain 3, regardless of mana.
- Cleaved Lantern Flare, choose X = 2 with `{2}{R}{W}` available → 2 damage / gain 2.
- Cleaved with insufficient mana for the chosen X → illegal / clamped by `maxAffordableX`.

---

## Rule references

- **Cleave** — CR 702.148 (alternative cost that removes the bracketed text). Implemented.
- **Maximum hand size** — CR 402.2 (default 7; cleanup discards down to it).

*(Rule numbers cross-checked against the numbers already cited in `MaximumHandSize.kt` and
`backlog/sets/innistrad-crimson-vow/mechanics.md`. Verify against the official Comprehensive Rules
before quoting them in new commit messages or code comments.)*
