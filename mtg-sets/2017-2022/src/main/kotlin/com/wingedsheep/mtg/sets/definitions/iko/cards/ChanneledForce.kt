package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Channeled Force
 * {2}{U}{R}
 * Instant
 * As an additional cost to cast this spell, discard X cards.
 * Target player draws X cards. Channeled Force deals X damage to up to one target creature or planeswalker.
 *
 * The number of cards discarded is the spell's X (X may be 0), read with `DynamicAmounts.xValue()`.
 */
val ChanneledForce = card("Channeled Force") {
    manaCost = "{2}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, discard X cards.\n" +
        "Target player draws X cards. Channeled Force deals X damage to up to one target creature or planeswalker."

    additionalCost(Costs.additional.DiscardVariableCards())

    spell {
        val player = target(Targets.Player)
        val victim = target(Targets.CreatureOrPlaneswalker, optional = true)
        effect = Effects.DrawCards(DynamicAmounts.xValue(), player) then
            Effects.DealDamage(DynamicAmounts.xValue(), victim)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "180"
        artist = "Randy Vargas"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/8918d24e-8ae8-4028-9f06-ba6fafcc717e.jpg?1783931027"
    }
}
