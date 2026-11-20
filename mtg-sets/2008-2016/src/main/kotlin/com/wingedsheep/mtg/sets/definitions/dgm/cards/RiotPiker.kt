package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MustAttack

/**
 * Riot Piker
 * {1}{R}
 * Creature — Goblin Berserker
 * 2/1
 * First strike
 * This creature attacks each combat if able.
 *
 * Canonical printing: Dragon's Maze, the card's earliest real printing.
 */
val RiotPiker = card("Riot Piker") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Berserker"
    power = 2
    toughness = 1
    oracleText = "First strike\n" +
        "This creature attacks each combat if able."

    keywords(Keyword.FIRST_STRIKE)

    staticAbility {
        ability = MustAttack()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "37"
        artist = "Christopher Moeller"
        flavorText = "He doesn't care whether he's rioting about trade rights or just having a drunken brawl. He wakes up every morning eager to smash some heads."
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4daaccd2-733c-4b3b-aa3f-cc825bcc3e53.jpg"
    }
}
