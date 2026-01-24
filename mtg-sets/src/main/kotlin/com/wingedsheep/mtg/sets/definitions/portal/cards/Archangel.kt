package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Archangel
 * {5}{W}{W}
 * Creature — Angel
 * 5/5
 * Flying, vigilance
 */
val Archangel = card("Archangel") {
    manaCost = "{5}{W}{W}"
    typeLine = "Creature — Angel"
    power = 5
    toughness = 5
    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "3"
        artist = "Quinton Hoover"
        imageUri = "https://cards.scryfall.io/normal/front/3/8/387b9236-1241-44b7-9436-1fbc9970b692.jpg"
    }
}
