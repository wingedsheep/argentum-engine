package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
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
 * produce exactly the same ability. The id is keyword-scoped rather than minted from the card being
 * built, because the engine builds the granted one mid-game, with no card around it; the cost is part
 * of it so a card with printed embalm that is granted a different embalm keeps the two apart.
 */
fun embalmAbility(cost: ManaCost): ActivatedAbility = ActivatedAbility(
    id = AbilityId("embalm_$cost"),
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
    keyword = Keyword.EMBALM,
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

/**
 * The Eternalize ability (CR 702.129a):
 *
 *   "[cost], Exile this card from your graveyard: Create a token that's a copy of it, except it's
 *    black, it's 4/4, it has no mana cost, and it's a Zombie in addition to its other types.
 *    Activate only as a sorcery."
 *
 * The 4/4 black sibling of [embalmAbility], composed the same way — mana [cost] plus
 * [AbilityCost.ExileSelf], `activateFromZone = Zone.GRAVEYARD`, sorcery timing, and a
 * [CreateTokenCopyOfTargetEffect] of the card itself carrying the printed exceptions. It is marked
 * `keyword = Keyword.ETERNALIZE`, so "whenever you activate an eternalize … ability" (Vizier of the
 * Anointed) can name it.
 *
 * [additionalCost] is the non-mana part of an eternalize cost such as Sunscourge Champion's
 * "Eternalize—{2}{W}{W}, Discard a card": it is paid alongside the mana and the self-exile, and
 * the ability keeps its [Keyword.ETERNALIZE] marker, so it is still an eternalize ability.
 */
fun eternalizeAbility(cost: ManaCost, additionalCost: AbilityCost? = null): ActivatedAbility = ActivatedAbility(
    id = AbilityId(if (additionalCost == null) "eternalize_$cost" else "eternalize_${cost}_${additionalCost.description}"),
    cost = AbilityCost.Composite(
        listOfNotNull(AbilityCost.Atom(CostAtom.Mana(cost)), additionalCost, AbilityCost.ExileSelf)
    ),
    effect = CreateTokenCopyOfTargetEffect(
        target = EffectTarget.Self,
        overridePower = 4,
        overrideToughness = 4,
        overrideColors = setOf(Color.BLACK),
        addedSubtypes = setOf(Subtype("Zombie")),
        exceptions = CopyExceptions(noManaCost = true),
    ),
    timing = TimingRule.SorcerySpeed,
    activateFromZone = Zone.GRAVEYARD,
    keyword = Keyword.ETERNALIZE,
    descriptionOverride = eternalizeCostText(cost, additionalCost).let { costText ->
        "Eternalize${if (additionalCost == null) " " else "—"}$costText ($costText, Exile this card from your " +
            "graveyard: Create a token that's a copy of it, except it's a 4/4 black Zombie with no mana cost. " +
            "Eternalize only as a sorcery.)"
    },
)

/**
 * Add Eternalize—[cost] (CR 702.129, Hour of Devastation) to a creature card.
 *
 * ```kotlin
 * eternalize("{5}{G}{G}")
 * ```
 *
 * A non-mana part of the cost goes in [additionalCost] — Sunscourge Champion's
 * "Eternalize—{2}{W}{W}, Discard a card" is `eternalize("{2}{W}{W}", Costs.DiscardCard)`.
 *
 * See [eternalizeAbility] for how the ability is composed.
 */
fun CardBuilder.eternalize(cost: String, additionalCost: AbilityCost? = null) {
    activatedAbilities.add(eternalizeAbility(ManaCost.parse(cost), additionalCost))
    keywordSet.add(Keyword.ETERNALIZE)
}

private fun eternalizeCostText(cost: ManaCost, additionalCost: AbilityCost?): String =
    if (additionalCost == null) "$cost" else "$cost, ${additionalCost.description}"
