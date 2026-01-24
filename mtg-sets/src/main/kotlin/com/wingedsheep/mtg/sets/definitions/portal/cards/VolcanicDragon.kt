package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Volcanic Dragon
 * {4}{R}{R}
 * Creature — Dragon
 * 4/4
 * Flying, haste
 */
val VolcanicDragon = card("Volcanic Dragon") {
    manaCost = "{4}{R}{R}"
    typeLine = "Creature — Dragon"
    power = 4
    toughness = 4

    keywords(Keyword.FLYING, Keyword.HASTE)

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "153"
        artist = "Tom Wänerstrand"
        flavorText = "Speed and fire make a deadly combination."
        imageUri = "https://cards.scryfall.io/normal/front/0/7/07b2c3d4-e5f6-a7b8-c9d0-e1f2a3b4c5d6.jpg"
    }
}
