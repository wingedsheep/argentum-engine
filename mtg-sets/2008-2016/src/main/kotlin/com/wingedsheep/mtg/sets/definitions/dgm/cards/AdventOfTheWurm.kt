package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Advent of the Wurm
 * {1}{G}{G}{W}
 * Instant
 * Create a 5/5 green Wurm creature token with trample.
 *
 * Canonical printing: Dragon's Maze, the card's earliest real printing.
 */
val AdventOfTheWurm = card("Advent of the Wurm") {
    manaCost = "{1}{G}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Instant"
    oracleText = "Create a 5/5 green Wurm creature token with trample."

    spell {
        effect = Effects.CreateToken(
            power = 5,
            toughness = 5,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Wurm"),
            keywords = setOf(Keyword.TRAMPLE),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "51"
        artist = "Lucas Graciano"
        flavorText = "The consciousness of Mat'Selesnya does not always spread in peaceful ways."
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f40284e6-01a1-4372-a92c-940e5732607e.jpg"
    }
}
