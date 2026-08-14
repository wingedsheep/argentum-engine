package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Unliving Legionnaire — Marvel Super Heroes #119 (common)
 * {3}{B} · Creature — Vampire Villain · 3/2
 *
 * Flying
 * Power-up — {5}{B}{B}: Return up to one target creature card from your graveyard to your hand.
 * Put two +1/+1 counters on this creature. (Activate each power-up ability only once. Reduce the
 * cost by its mana cost if it entered this turn.)
 *
 * "Up to one target" is `optional = true` (minimum count zero), so the ability is still
 * activatable with an empty graveyard — which on the turn it enters is the normal case, since the
 * discount pushes you to activate early. Both halves resolve in printed order.
 *
 * `{5}{B}{B}` − `{3}{B}` = `{2}{B}`.
 */
val UnlivingLegionnaire = card("Unliving Legionnaire") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Villain"
    oracleText = "Flying\n" +
        "Power-up — {5}{B}{B}: Return up to one target creature card from your graveyard to your " +
        "hand. Put two +1/+1 counters on this creature. (Activate each power-up ability only " +
        "once. Reduce the cost by its mana cost if it entered this turn.)"
    power = 3
    toughness = 2

    keywords(Keyword.FLYING)

    activatedAbility {
        isPowerUp = true
        cost = Costs.Mana("{5}{B}{B}")
        val corpse = target(
            "up to one target creature card from your graveyard",
            TargetObject(optional = true, filter = TargetFilter.CreatureInYourGraveyard)
        )
        effect = Effects.Composite(
            Effects.ReturnToHand(corpse),
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "119"
        artist = "Björn Barends"
        imageUri = "https://cards.scryfall.io/normal/front/c/e/ce08f4bb-7da3-4199-8b90-fcdb29e84e98.jpg?1783902935"
    }
}
