package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Jhovall Queen
 * {4}{W}{W}
 * Creature — Cat Rebel
 * 4 / 7
 *
 * Vigilance
 */
val JhovallQueen = card("Jhovall Queen") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat Rebel"
    oracleText = "Vigilance"
    power = 4
    toughness = 7
    keywords(Keyword.VIGILANCE)

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "25"
        artist = "Michael Sutfin"
        flavorText = "War-trained jhovalls eat twice their weight in war-trained soldiers daily."
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b8eb55cc-ddde-4f15-9262-b9aee28059d3.jpg"
    }
}
