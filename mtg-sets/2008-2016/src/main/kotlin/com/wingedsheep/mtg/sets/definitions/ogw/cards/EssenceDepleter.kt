package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Essence Depleter
 * {2}{B}
 * Creature — Eldrazi Drone
 * 2/3
 * Devoid (This card has no color.)
 * {1}{C}: Target opponent loses 1 life and you gain 1 life. ({C} represents colorless mana.)
 */
val EssenceDepleter = card("Essence Depleter") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Eldrazi Drone"
    power = 2
    toughness = 3
    oracleText = "Devoid (This card has no color.)\n" +
        "{1}{C}: Target opponent loses 1 life and you gain 1 life. ({C} represents colorless mana.)"

    keywords(Keyword.DEVOID)

    activatedAbility {
        cost = Costs.Mana("{1}{C}")
        val opponent = target(Targets.Opponent)
        effect = Effects.LoseLife(1, opponent) then Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "69"
        artist = "Chase Stone"
        flavorText = "When Kozilek emerged at Sea Gate, his brood lineage swarmed to the surface."
        imageUri = "https://cards.scryfall.io/normal/front/9/9/996da623-1c34-4545-a800-648238ea91ae.jpg"
    }
}
