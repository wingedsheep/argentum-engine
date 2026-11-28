package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Brain Weevil
 * {3}{B}
 * Creature — Insect
 * 1/1
 * Intimidate (This creature can't be blocked except by artifact creatures and/or creatures that share a color with it.)
 * Sacrifice this creature: Target player discards two cards. Activate only as a sorcery.
 */
val BrainWeevil = card("Brain Weevil") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Insect"
    oracleText = "Intimidate (This creature can't be blocked except by artifact creatures and/or creatures that share a color with it.)\nSacrifice this creature: Target player discards two cards. Activate only as a sorcery."
    power = 1
    toughness = 1
    keywords(Keyword.INTIMIDATE)
    activatedAbility {
        cost = Costs.SacrificeSelf
        val player = target(Targets.Player)
        effect = Patterns.Hand.discardCards(2, player)
        timing = TimingRule.SorcerySpeed
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "91"
        artist = "Anthony Jones"
        imageUri = "https://cards.scryfall.io/normal/front/4/0/40e1bd88-939a-4adc-8693-210a7ba9a5a1.jpg"
    }
}
