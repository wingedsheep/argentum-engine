package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock

/**
 * Kyren Glider
 * {1}{R}
 * Creature — Goblin
 * 1 / 1
 */
val KyrenGlider = card("Kyren Glider") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin"
    oracleText = "Flying\n" +
        "This creature can't block."
    power = 1
    toughness = 1

    keywords(Keyword.FLYING)

    staticAbility {
        ability = CantBlock()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "196"
        artist = "Daren Bader"
        flavorText = "Mercadia's Kyren goblins are the opposite of Dominarian goblins: they're smart and cowardly."
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0bc55e01-342e-4856-937e-14561b8d165b.jpg"
    }
}
