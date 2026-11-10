package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Prophet of Distortion
 * {U}
 * Creature — Eldrazi Drone
 * 1/2
 * Devoid (This card has no color.)
 * {3}{C}: Draw a card. ({C} represents colorless mana.)
 */
val ProphetOfDistortion = card("Prophet of Distortion") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Eldrazi Drone"
    power = 1
    toughness = 2
    oracleText = "Devoid (This card has no color.)\n" +
        "{3}{C}: Draw a card. ({C} represents colorless mana.)"

    keywords(Keyword.DEVOID)

    activatedAbility {
        cost = Costs.Mana("{3}{C}")
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "46"
        artist = "Lius Lasahido"
        flavorText = "It doesn't twist words. It twists worlds."
        imageUri = "https://cards.scryfall.io/normal/front/0/3/03ee2d7e-50ee-4f5a-b787-cf9669a892f3.jpg"
    }
}
