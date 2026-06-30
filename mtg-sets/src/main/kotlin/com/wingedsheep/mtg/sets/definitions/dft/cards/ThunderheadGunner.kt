package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Thunderhead Gunner
 * {4}{R}
 * Creature — Shark Pirate
 * 4/5
 * Reach
 * Discard a card: Draw a card. Activate only as a sorcery and only once each turn.
 */
val ThunderheadGunner = card("Thunderhead Gunner") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Shark Pirate"
    power = 4
    toughness = 5
    oracleText = "Reach\n" +
        "Discard a card: Draw a card. Activate only as a sorcery and only once each turn."
    keywords(Keyword.REACH)

    activatedAbility {
        cost = Costs.DiscardCard
        effect = Effects.DrawCards(1)
        timing = TimingRule.SorcerySpeed
        restrictions = listOf(ActivationRestriction.OncePerTurn)
        description = "Discard a card: Draw a card. Activate only as a sorcery and only once each turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "148"
        artist = "Mirko Failoni"
        imageUri = "https://cards.scryfall.io/normal/front/e/f/ef537868-b2cb-4bbd-a935-b7f73f19bd06.jpg"
    }
}
