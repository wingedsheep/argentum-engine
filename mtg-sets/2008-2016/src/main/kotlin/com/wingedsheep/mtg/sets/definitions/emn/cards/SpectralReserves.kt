package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Spectral Reserves
 * {3}{W}
 * Sorcery
 * Create two 1/1 white Spirit creature tokens with flying. You gain 2 life.
 */
val SpectralReserves = card("Spectral Reserves") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Create two 1/1 white Spirit creature tokens with flying. You gain 2 life."

    spell {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Spirit"),
            keywords = setOf(Keyword.FLYING),
            count = 2
        ) then Effects.GainLife(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "43"
        artist = "Seb McKinnon"
        flavorText = "\"My predecessors taught me of duty and commitment. It seems they have more to teach.\"\n" +
            "—Wendin, veteran cathar"
        imageUri = "https://cards.scryfall.io/normal/front/7/6/7618aa2d-d5b3-41e6-978f-2c8292f7f896.jpg"
    }
}
