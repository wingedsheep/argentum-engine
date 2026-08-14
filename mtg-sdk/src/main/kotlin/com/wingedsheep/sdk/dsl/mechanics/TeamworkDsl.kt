package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Add Teamwork [n] (CR 702.194, Marvel Super Heroes) — "As an additional cost to cast this spell,
 * you may tap any number of creatures you control with total power [n] or more."
 *
 * A static ability that functions while the spell is on the stack (CR 702.194a). Declaring the
 * intention as the spell is cast (CR 601.2b) means the spell was cast *using teamwork*
 * (CR 702.194b) — a fact the card's own riders branch on, carried by the spell on the stack and
 * stamped durably on the permanent it becomes.
 *
 * Wired entirely by the shared optional-additional-cost rail
 * ([KeywordAbility.OptionalAdditionalCost]) with `declaredSlot = `[ChoiceSlot.TEAMWORK], exactly as
 * `bargain()` rides it with [ChoiceSlot.BARGAINED]: the enumerator offers a
 * "Cast … (Teamwork [n])" variant whenever the caster's untapped creatures can reach the
 * threshold, the cast handler collects the tapped creatures through the ordinary additional-cost
 * payment flow, and the engine stamps [ChoiceSlot.TEAMWORK]. Because the slot — not a shared
 * boolean — carries the mechanic's identity, a teamwork spell never satisfies a "whenever you cast
 * a kicked spell" payoff and vice versa (CR 702.194b).
 *
 * The cost itself is `Costs.additional.TapForTotalPower(n)`: the same "tap any number of creatures
 * you control with total power N or greater" selection crew (CR 702.122a) and saddle already use,
 * measured against **projected** power so a lord bonus or a +1/+1 counter counts. Tapping as a cost
 * is not the `{T}` symbol, so summoning sickness (CR 302.6) never applies.
 *
 * The card supplies its own payoff; teamwork derives nothing:
 * - A **plain rider** — gate the extra clause on [Conditions.TeamworkWasPaid]
 *   (Helicarrier Strike: "If this spell was cast using teamwork, it deals 4 damage instead").
 * - A **modal "choose both instead"** (CR 700.2 for the mode count, declared per CR 601.2b) —
 *   [teamworkModal], which is a `modal { }` block wired for exactly that wording.
 * - A **teamwork-only clause with its own target** (CR 702.194c) — the rail's shared `kickerEffect`
 *   / `kickerTarget` slots in the `spell { }` block, which serve whichever mechanic declared, so
 *   the plain cast is announced as though the clause weren't there.
 */
fun CardBuilder.teamwork(n: Int) {
    keywordAbilityList.add(
        KeywordAbility.OptionalAdditionalCost(
            additionalCost = Costs.additional.TapForTotalPower(n, GameObjectFilter.Creature),
            displayPrefix = "Teamwork $n",
            keyword = Keyword.TEAMWORK,
            declaredSlot = ChoiceSlot.TEAMWORK,
        )
    )
}

/**
 * "Choose one. If this spell was cast using teamwork, choose both instead." — the modal payoff
 * shape, for a spell that also declares [teamwork].
 *
 * The plain cast chooses one mode; the declared cast chooses *all* of them, and the "instead"
 * makes that mandatory rather than an allowance. Both bounds therefore ride the same
 * [DynamicAmount.Conditional] on [Conditions.TeamworkWasPaid]: a ceiling alone would let a player
 * tap their team and then take a single mode.
 *
 * "Both" is read off the modes actually declared, so a three-mode printing needs no new recipe.
 * The count is decided at cast time from the declaration this very cast made (CR 601.2b), not from
 * the battlefield — see [ModalEffect.dynamicChooseCount] for why that needs `declaredCostSlot` in
 * the evaluation context.
 */
fun SpellBuilder.teamworkModal(init: ModalBuilder.() -> Unit) {
    val builder = ModalBuilder(chooseCount = 1, minChooseCount = 1)
    builder.init()
    val base = builder.build()
    val bothIfTeamwork = DynamicAmount.Conditional(
        condition = Conditions.TeamworkWasPaid,
        ifTrue = DynamicAmount.Fixed(base.modes.size),
        ifFalse = DynamicAmount.Fixed(1),
    )
    effect = base.copy(
        chooseCount = base.modes.size,
        minChooseCount = 1,
        dynamicChooseCount = bothIfTeamwork,
        dynamicMinChooseCount = bothIfTeamwork,
    )
}
