package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Winds of Change
 * {R}
 * Sorcery
 * Each player shuffles the cards from their hand into their library, then draws that many cards.
 */
val WindsOfChange = card("Winds of Change") {
    manaCost = "{R}"
    typeLine = "Sorcery"

    spell {
        effect = Effects.WindsOfChange()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "156"
        artist = "NéNé Thomas"
        flavorText = "Change comes like the wind—swift and unpredictable."
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3ae5f6a7-b8c9-d0e1-f2a3-b4c5d6e7f8a9.jpg"
    }
}
