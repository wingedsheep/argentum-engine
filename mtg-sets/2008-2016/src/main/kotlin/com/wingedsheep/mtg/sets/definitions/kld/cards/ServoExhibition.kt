package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Servo Exhibition
 * {1}{W}
 * Sorcery
 * Create two 1/1 colorless Servo artifact creature tokens.
 */
val ServoExhibition = card("Servo Exhibition") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Create two 1/1 colorless Servo artifact creature tokens."

    spell {
        effect = Effects.CreateToken(
            count = 2,
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Servo"),
            artifactToken = true,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "27"
        artist = "Sara Winters"
        flavorText = "\"The judges really put them through their paces, but they prevailed. I think they even impressed Head Judge Tezzeret.\"\n—Ritika, artificer"
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9aba0c60-78e2-4e1c-a6ad-6f1c001ad7ee.jpg?1783937228"
    }
}
