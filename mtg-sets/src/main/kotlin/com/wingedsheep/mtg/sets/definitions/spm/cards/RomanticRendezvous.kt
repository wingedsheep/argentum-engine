package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Romantic Rendezvous
 * {1}{R}
 * Sorcery
 * Discard a card, then draw two cards.
 */
val RomanticRendezvous = card("Romantic Rendezvous") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Discard a card, then draw two cards."

    spell {
        effect = Effects.Discard() then Effects.DrawCards(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "86"
        artist = "Dmitry Burmak"
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b7e1c2ef-6e83-4b87-84c6-7dbf5dec8e50.jpg?1757377869"
    }
}
