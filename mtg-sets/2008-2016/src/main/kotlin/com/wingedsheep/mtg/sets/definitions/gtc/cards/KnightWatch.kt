package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Knight Watch
 * {4}{W}
 * Sorcery
 * Create two 2/2 white Knight creature tokens with vigilance.
 *
 * Gatecrash printed no Knight token of its own (its tokens came from the Return to Ravnica
 * token sheet), so the RTR Knight token art is baked in rather than left to GTC's registry.
 */
val KnightWatch = card("Knight Watch") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Create two 2/2 white Knight creature tokens with vigilance."

    spell {
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Knight"),
            keywords = setOf(Keyword.VIGILANCE),
            count = 2,
            imageUri = "https://cards.scryfall.io/normal/front/6/7/67d3d039-248a-4eb8-be5c-12959b458fea.jpg?1783940383"
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "19"
        artist = "Matt Stewart"
        flavorText = "\"We lost respect after the battle for Barbu Rooftop. Send the troops to mingle respectfully with the citizens.\"\n—Commander Yaszen of Boros"
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cd492072-9a8c-4d55-ac71-3c8efaa3fc87.jpg"
    }
}
