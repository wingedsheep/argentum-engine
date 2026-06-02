package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.HandPatterns

/**
 * Temporary Truce
 * {1}{W}
 * Sorcery
 * Each player may draw up to two cards. For each card less than two a player
 * draws this way, that player gains 2 life.
 */
val TemporaryTruce = card("Temporary Truce") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"

    spell {
        effect = HandPatterns.eachPlayerMayDraw(maxCards = 2, lifePerCardNotDrawn = 2)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "33"
        artist = "Mike Raabe"
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5f6ee294-7dbb-4872-81d1-c69c7337cf9f.jpg"
    }
}
