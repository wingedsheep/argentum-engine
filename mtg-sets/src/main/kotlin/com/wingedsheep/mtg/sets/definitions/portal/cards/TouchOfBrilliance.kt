package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DrawCardsEffect

/**
 * Touch of Brilliance
 * {3}{U}
 * Sorcery
 * Draw two cards.
 */
val TouchOfBrilliance = card("Touch of Brilliance") {
    manaCost = "{3}{U}"
    typeLine = "Sorcery"

    spell {
        effect = DrawCardsEffect(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "76"
        artist = "Douglas Shuler"
        imageUri = "https://cards.scryfall.io/normal/front/8/0/80fe6b91-1078-4b36-824d-1defc29d5f3c.jpg"
    }
}
