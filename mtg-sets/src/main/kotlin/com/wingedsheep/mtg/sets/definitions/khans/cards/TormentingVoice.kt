package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalCost

/**
 * Tormenting Voice
 * {1}{R}
 * Sorcery
 * As an additional cost to cast this spell, discard a card.
 * Draw two cards.
 */
val TormentingVoice = card("Tormenting Voice") {
    manaCost = "{1}{R}"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, discard a card.\nDraw two cards."

    additionalCost(AdditionalCost.DiscardCards())

    spell {
        effect = Effects.DrawCards(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "123"
        artist = "Volkan Baǵa"
        flavorText = "\"Unwelcome thoughts crowd my mind. Are they my own madness, or the whispers of another?\"\n—Sarkhan Vol"
        imageUri = "https://cards.scryfall.io/normal/front/2/5/25af9ac1-a03b-4be7-b726-fb66427b1caa.jpg?1562783820"
    }
}
