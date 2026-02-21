package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Wall of Granite
 * {2}{R}
 * Creature — Wall
 * 0/7
 * Defender
 */
val WallOfGranite = card("Wall of Granite") {
    manaCost = "{2}{R}"
    typeLine = "Creature — Wall"
    power = 0
    toughness = 7

    keywords(Keyword.DEFENDER)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "155"
        artist = "John Matson"
        flavorText = "Solid as the mountain from which it came."
        imageUri = "https://cards.scryfall.io/normal/front/7/0/70c5ac71-bf45-4b99-8184-36ce88dd728a.jpg"
        ruling("2004-10-04", "As of the Champions of Kamigawa rules update, the Wall creature type no longer inherently prevents attacking. All Walls printed before this update received errata granting the defender keyword.")
    }
}
