package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect

/**
 * Call the Cavalry
 * {3}{W}
 * Sorcery
 * Create two 2/2 white Knight creature tokens with vigilance.
 */
val CallTheCavalry = card("Call the Cavalry") {
    manaCost = "{3}{W}"
    typeLine = "Sorcery"
    oracleText = "Create two 2/2 white Knight creature tokens with vigilance."

    spell {
        effect = CreateTokenEffect(
            count = 2,
            power = 2,
            toughness = 2,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Knight"),
            keywords = setOf(Keyword.VIGILANCE),
            imageUri = "https://cards.scryfall.io/normal/front/e/0/e0bce908-fc95-40c6-a04a-752d56aca836.jpg?1562702400"
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "9"
        artist = "Scott Murphy"
        flavorText = "\"Benalish citizens born under the same constellations share a star-clan. Their loyalty to one another interlaces the Seven Houses.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/9/7978a719-b159-4155-88d9-9c0b15183037.jpg?1562738110"
    }
}
