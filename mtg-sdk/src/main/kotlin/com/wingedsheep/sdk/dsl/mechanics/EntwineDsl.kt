package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Add Entwine [cost] (CR 702.42, Mirrodin) — "Entwine {3} (Choose all if you pay the entwine cost.)"
 *
 * A static ability of modal spells that functions while the spell is on the stack (CR 702.42a):
 * "You may choose all modes of this spell instead of just the number specified. If you do, you pay
 * an additional [cost]." Declaring that intention as the spell is cast (CR 601.2b) makes it
 * *entwined*.
 *
 * Wired entirely by the shared optional-additional-cost rail
 * ([KeywordAbility.OptionalAdditionalCost]) with `declaredSlot = `[ChoiceSlot.ENTWINED], exactly as
 * `bargain()` and `teamwork(n)` ride it: the enumerator offers a "Cast … (Entwined)" variant next
 * to the plain cast, the cast handler folds the cost into the spell's price **once**, and the
 * engine stamps [ChoiceSlot.ENTWINED] on the spell. Because the slot — not a shared boolean —
 * carries the mechanic's identity, an entwined spell never satisfies a "whenever you cast a kicked
 * spell" payoff.
 *
 * That "pay once, take all" shape is why entwine is *not* escalate
 * (`additionalManaCostPerExtraMode`): escalate charges for every mode beyond the first, which
 * equals entwine only for a two-mode "choose one" and overcharges everything else — Kaya's Guile
 * chooses two of four, so entwining takes two *more* modes for a single {3}.
 *
 * Pair it with [entwineModal] on the spell, which is what moves the mode count.
 *
 * @param cost The entwine cost, as a mana string (`"{3}"`).
 */
fun CardBuilder.entwine(cost: String) {
    keywordAbilityList.add(
        KeywordAbility.OptionalAdditionalCost(
            manaCost = com.wingedsheep.sdk.core.ManaCost.parse(cost),
            displayPrefix = "Entwine",
            keyword = Keyword.ENTWINE,
            declaredSlot = ChoiceSlot.ENTWINED,
        )
    )
}

/**
 * "Choose [chooseCount] — … Entwine [cost]" — the modal shape of a spell that also declares
 * [entwine].
 *
 * The plain cast chooses exactly [chooseCount] modes; the entwined cast chooses *all* of them
 * (CR 702.42a). Both ends of the count ride the same [DynamicAmount.Conditional] on
 * [Conditions.WasEntwined], so the legal counts are exactly `{chooseCount, modes.size}` — a range
 * (`chooseCount = 4, minChooseCount = 2`) would also admit three modes, which no entwine card
 * allows. The count is decided at cast time from the declaration this cast made, not from the
 * battlefield — see [ModalEffect.dynamicChooseCount].
 *
 * Use `chooseCount = 1` (the default) for the classic "Choose one — … Entwine {2}" printing, and
 * `chooseCount = 2` for "Choose two — … Entwine {3}" (Kaya's Guile).
 */
fun SpellBuilder.entwineModal(chooseCount: Int = 1, init: ModalBuilder.() -> Unit) {
    modalChoosingAllIf(Conditions.WasEntwined, chooseCount, init)
}

/**
 * The shared body of the "choose N; if [condition] was declared, choose all instead" modal shapes
 * ([entwineModal], [teamworkModal]).
 *
 * "Instead" is mandatory rather than an allowance, so both bounds ride one conditional amount: a
 * ceiling alone would let a player pay the extra cost and still take fewer modes. "All" is read off
 * the modes actually declared, so a three-mode printing needs no new recipe.
 */
internal fun SpellBuilder.modalChoosingAllIf(
    condition: Condition,
    chooseCount: Int,
    init: ModalBuilder.() -> Unit,
) {
    val builder = ModalBuilder(chooseCount = chooseCount, minChooseCount = chooseCount)
    builder.init()
    val base = builder.build()
    require(chooseCount < base.modes.size) {
        "Choosing all modes only differs from choosing $chooseCount when the spell has more " +
            "than $chooseCount modes; got ${base.modes.size}"
    }
    val allIfDeclared = DynamicAmount.Conditional(
        condition = condition,
        ifTrue = DynamicAmount.Fixed(base.modes.size),
        ifFalse = DynamicAmount.Fixed(chooseCount),
    )
    effect = base.copy(
        chooseCount = base.modes.size,
        minChooseCount = chooseCount,
        dynamicChooseCount = allIfDeclared,
        dynamicMinChooseCount = allIfDeclared,
    )
}
