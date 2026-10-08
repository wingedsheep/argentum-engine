package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Mass Production
 * {5}{W}
 * Sorcery
 * Create four 1/1 colorless Soldier artifact creature tokens.
 */
val MassProduction = card("Mass Production") {
    manaCost = "{5}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Create four 1/1 colorless Soldier artifact creature tokens."

    spell {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Soldier"),
            count = 4,
            artifactToken = true
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "15"
        artist = "Rémi Jacquot"
        flavorText = "\"I need an army, not a masterpiece. Accelerate output, whatever it takes.\"\n—Urza, notes to his engineers"
        imageUri = "https://cards.scryfall.io/normal/front/6/4/640a08b7-dd30-446e-a8a7-2e084bbb9586.jpg"
    }
}
