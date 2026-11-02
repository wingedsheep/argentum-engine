package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Kraul Raider
 * {2}{B}
 * Creature — Insect Warrior
 * 2/3
 * Menace (This creature can't be blocked except by two or more creatures.)
 */
val KraulRaider = card("Kraul Raider") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Insect Warrior"
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)"
    power = 2
    toughness = 3

    keywords(Keyword.MENACE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "270"
        inBooster = false
        artist = "Ben Wootten"
        flavorText = "The kraul once skulked in the outskirts of Golgari society, but with Vraska's rise, they became valued forces of the Swarm."
        imageUri = "https://cards.scryfall.io/normal/front/1/3/133d9d56-d906-4252-9954-e34cc8564ced.jpg?1783934094"
    }
}
