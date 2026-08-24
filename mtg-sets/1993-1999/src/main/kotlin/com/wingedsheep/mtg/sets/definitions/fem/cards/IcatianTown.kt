package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Icatian Town
 * {5}{W}
 * Sorcery
 * Create four 1/1 white Citizen creature tokens.
 *
 * The Citizen art is registered on [com.wingedsheep.mtg.sets.definitions.fem.FallenEmpiresSet].
 */
val IcatianTown = card("Icatian Town") {
    manaCost = "{5}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Create four 1/1 white Citizen creature tokens."

    spell {
        effect = Effects.CreateToken(
            count = 4,
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Citizen")
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "15"
        artist = "Tom Wänerstrand"
        flavorText = "Icatia's once peaceful towns faced increasing attacks from Orcs and Goblins as the climate cooled. By the time the empire fell, they were little more than armed camps."
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cbb7c28d-0366-4d01-84a2-f1bc9f38aa4a.jpg?1783947914"
    }
}
