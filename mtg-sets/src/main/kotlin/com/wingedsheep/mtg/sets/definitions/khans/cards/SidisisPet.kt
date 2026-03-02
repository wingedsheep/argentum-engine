package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Sidisi's Pet
 * {3}{B}
 * Creature — Zombie Ape
 * 1/4
 * Lifelink
 * Morph {1}{B}
 */
val SidisisPet = card("Sidisi's Pet") {
    manaCost = "{3}{B}"
    typeLine = "Creature — Zombie Ape"
    power = 1
    toughness = 4
    oracleText = "Lifelink (Damage dealt by this creature also causes you to gain that much life.)\nMorph {1}{B} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)"

    keywords(Keyword.LIFELINK)

    morph = "{1}{B}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "90"
        artist = "Evan Shipard"
        flavorText = "The Sultai distinguish between pet and slave by the material of the chain."
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e6e02a99-da37-4cea-9182-39ccb1e35ee9.jpg?1562795181"
    }
}
