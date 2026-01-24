package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EachPlayerMayDrawEffect

/**
 * Temporary Truce
 * {1}{W}
 * Sorcery
 * Each player may draw up to two cards.
 */
val TemporaryTruce = card("Temporary Truce") {
    manaCost = "{1}{W}"
    typeLine = "Sorcery"

    spell {
        effect = EachPlayerMayDrawEffect(maxCards = 2)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "33"
        artist = "Mike Raabe"
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5f6ee294-7dbb-4872-81d1-c69c7337cf9f.jpg"
    }
}
