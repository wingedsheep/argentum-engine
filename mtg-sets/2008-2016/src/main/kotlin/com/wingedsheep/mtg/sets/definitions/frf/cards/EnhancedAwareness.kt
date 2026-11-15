package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Enhanced Awareness
 * {4}{U}
 * Instant
 * Draw three cards, then discard a card.
 */
val EnhancedAwareness = card("Enhanced Awareness") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Draw three cards, then discard a card."

    spell {
        effect = Effects.DrawCards(3) then Patterns.Hand.discardCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "33"
        artist = "Ryan Alexander Lee"
        flavorText = "\"Study the topography of your enemies, and you will have a map to victory.\"\n—Shu Yun, the Silent Tempest"
        imageUri = "https://cards.scryfall.io/normal/front/2/1/21df7db7-c23b-4dca-b9c8-c785d7007285.jpg"
    }
}
