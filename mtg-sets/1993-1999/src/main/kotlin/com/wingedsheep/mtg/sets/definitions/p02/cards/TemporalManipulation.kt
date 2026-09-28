package com.wingedsheep.mtg.sets.definitions.p02.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Temporal Manipulation
 * {3}{U}{U}
 * Sorcery
 * Take an extra turn after this one.
 */
val TemporalManipulation = card("Temporal Manipulation") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Take an extra turn after this one."

    spell {
        effect = Effects.TakeExtraTurn()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "54"
        artist = "Anson Maddocks"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3964160-79d6-4cdd-8b43-7a8f5dde9da7.jpg?1783946483"
    }
}
