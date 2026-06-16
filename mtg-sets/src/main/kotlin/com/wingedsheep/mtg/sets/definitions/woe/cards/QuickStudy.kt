package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Quick Study
 * {2}{U}
 * Instant
 *
 * Draw two cards.
 */
val QuickStudy = card("Quick Study") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Draw two cards."

    spell {
        effect = Effects.DrawCards(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "65"
        artist = "Iris Compiet"
        flavorText = "At first, Johann was annoyed that the library had decided to reorganize itself. But he had to admit that storing the cookbooks in the cauldron made a certain kind of sense."
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b78e2bca-bc93-464a-8911-8361abff2ac6.jpg?1692937231"
    }
}
