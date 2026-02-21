package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Wall of Swords
 * {3}{W}
 * Creature - Wall
 * 3/5
 * Defender, flying
 */
val WallOfSwords = card("Wall of Swords") {
    manaCost = "{3}{W}"
    typeLine = "Creature — Wall"
    power = 3
    toughness = 5

    keywords(Keyword.DEFENDER, Keyword.FLYING)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "37"
        artist = "Douglas Shuler"
        flavorText = "\"Sharper than wind, lighter than air.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3e8d55a3-0d7f-4fba-9879-9a8264110e78.jpg"
        ruling("2004-10-04", "As of the Champions of Kamigawa rules update, the Wall creature type no longer inherently prevents attacking. All Walls printed before this update received errata granting the defender keyword.")
    }
}
