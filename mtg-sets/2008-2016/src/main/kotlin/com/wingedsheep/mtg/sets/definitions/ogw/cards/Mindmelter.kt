package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Mindmelter
 * {1}{U}{B}
 * Creature — Eldrazi Drone
 * 2/2
 * Devoid (This card has no color.)
 * This creature can't be blocked.
 * {3}{C}: Target opponent exiles a card from their hand. Activate only as a sorcery.
 * ({C} represents colorless mana.)
 */
val Mindmelter = card("Mindmelter") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "BU"
    typeLine = "Creature — Eldrazi Drone"
    power = 2
    toughness = 2
    oracleText = "Devoid (This card has no color.)\n" +
        "This creature can't be blocked.\n" +
        "{3}{C}: Target opponent exiles a card from their hand. Activate only as a sorcery. " +
        "({C} represents colorless mana.)"

    keywords(Keyword.DEVOID)
    flags(AbilityFlag.CANT_BE_BLOCKED)

    activatedAbility {
        cost = Costs.Mana("{3}{C}")
        val opponent = target(Targets.Opponent)
        effect = Patterns.Hand.exileFromHand(1, opponent)
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "149"
        artist = "Jason Felix"
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4a5bf575-6d09-4cb0-8fe0-8e49d2c39fd5.jpg"
    }
}
