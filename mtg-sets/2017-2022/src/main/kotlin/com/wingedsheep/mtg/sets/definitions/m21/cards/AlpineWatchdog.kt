package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Alpine Watchdog
 * {1}{W}
 * Creature — Dog
 * 2/2
 * Vigilance
 */
val AlpineWatchdog = card("Alpine Watchdog") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dog"
    power = 2
    toughness = 2
    oracleText = "Vigilance (Attacking doesn't cause this creature to tap.)"

    keywords(Keyword.VIGILANCE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "2"
        artist = "Forrest Imel"
        flavorText = "\"On the eighth day, a blizzard hit. Supplies were lost, and morale plummeted. " +
            "On the ninth day, Dover found us.\"\n—*To the Summit: A Tale of Survival*"
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c392a7e5-6ff5-4c2f-9590-f8811a724f44.jpg?1783930747"
    }
}
