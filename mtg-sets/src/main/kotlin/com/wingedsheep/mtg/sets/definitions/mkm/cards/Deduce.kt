package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Deduce
 * {1}{U}
 * Instant
 * Draw a card. Investigate.
 */
val Deduce = card("Deduce") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Draw a card. Investigate. (Create a Clue token. It's an artifact with " +
        "\"{2}, Sacrifice this token: Draw a card.\")"

    spell {
        effect = Effects.Composite(
            Effects.DrawCards(1),
            Effects.Investigate()
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "52"
        artist = "Quintin Gleim"
        flavorText = "Proft lit up like a man who had just been handed a glorious and unexpected gift. " +
            "\"I know who's responsible for this,\" he said. \"And I can prove it.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7cbb17af-e17e-438a-ad72-0c942e6706b6.jpg?1783912912"
    }
}
