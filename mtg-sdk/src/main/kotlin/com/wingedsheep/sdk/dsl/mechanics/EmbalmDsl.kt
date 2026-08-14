package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfTargetEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Embalm ability (CR 702.128a):
 *
 *   "[cost], Exile this card from your graveyard: Create a token that's a copy of it, except it's
 *    a white Zombie in addition to its other types and it has no mana cost. Activate only as a
 *    sorcery."
 *
 * Like [renew], embalm needs no engine subsystem of its own — it is an ordinary graveyard-activated
 * ability composed of existing primitives:
 *  - the mana [cost] plus [AbilityCost.ExileSelf] (the card exiles itself from the graveyard as
 *    part of the cost, which is why per the Cursecloth Wrappings ruling an opponent can't respond
 *    by exiling the card),
 *  - `activateFromZone = Zone.GRAVEYARD`, so the engine's `ZoneActivatedAbilityEnumerator` surfaces
 *    it while the card is in the graveyard,
 *  - `timing = TimingRule.SorcerySpeed` for "Activate only as a sorcery", and
 *  - a [CreateTokenCopyOfTargetEffect] of the card itself carrying the three printed exceptions:
 *    white ([CreateTokenCopyOfTargetEffect.overrideColors] — the token is white *instead of* its
 *    other colors), Zombie in addition to its other types
 *    ([CreateTokenCopyOfTargetEffect.addedSubtypes]), and no mana cost
 *    ([CreateTokenCopyOfTargetEffect.noManaCost]).
 *
 * The source resolves through [EffectTarget.Self] — by the time the effect runs the card is in
 * exile, and the copy is made from what was printed on it, not from anything it was on the
 * battlefield.
 *
 * This factory is shared by printed embalm ([embalm] below) and the runtime grant
 * ([com.wingedsheep.sdk.scripting.effects.GrantEmbalmEffect], Cursecloth Wrappings), so both
 * produce exactly the same ability.
 */
fun embalmAbility(cost: ManaCost): ActivatedAbility = ActivatedAbility(
    cost = AbilityCost.Composite(
        listOf(AbilityCost.Atom(CostAtom.Mana(cost)), AbilityCost.ExileSelf)
    ),
    effect = CreateTokenCopyOfTargetEffect(
        target = EffectTarget.Self,
        overrideColors = setOf(Color.WHITE),
        addedSubtypes = setOf(Subtype("Zombie")),
        noManaCost = true,
    ),
    timing = TimingRule.SorcerySpeed,
    activateFromZone = Zone.GRAVEYARD,
    descriptionOverride = "Embalm $cost (Exile this card from your graveyard and pay its embalm " +
        "cost: Create a token that's a copy of it, except it's a white Zombie in addition to its " +
        "other types and has no mana cost. Embalm only as a sorcery.)",
)

/**
 * Add Embalm—[cost] (CR 702.128, Amonkhet) to a creature card.
 *
 * ```kotlin
 * embalm("{3}{W}")
 * ```
 *
 * See [embalmAbility] for how the ability is composed.
 */
fun CardBuilder.embalm(cost: String) {
    activatedAbilities.add(embalmAbility(ManaCost.parse(cost)))
    keywordSet.add(Keyword.EMBALM)
}
