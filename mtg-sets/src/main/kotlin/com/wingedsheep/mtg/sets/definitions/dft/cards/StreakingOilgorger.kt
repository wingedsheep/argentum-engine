package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.maxSpeed
import com.wingedsheep.sdk.dsl.startYourEngines
import com.wingedsheep.sdk.model.Rarity

/**
 * Streaking Oilgorger — Aetherdrift #107
 * {4}{B} · Creature — Vampire · 3/3
 *
 * Flying, haste
 * Start your engines!
 * Max speed — This creature has lifelink.
 */
val StreakingOilgorger = card("Streaking Oilgorger") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire"
    power = 3
    toughness = 3
    oracleText = "Flying, haste\n" +
        "Start your engines! (If you have no speed, it starts at 1. It increases once on each of " +
        "your turns when an opponent loses life. Max speed is 4.)\n" +
        "Max speed — This creature has lifelink."

    keywords(Keyword.FLYING, Keyword.HASTE)
    startYourEngines()
    maxSpeed { keywords(Keyword.LIFELINK) }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "107"
        artist = "Campbell White"
        flavorText = "\"They feed on OIL? Where were they during the Phyrexian Invasion?!\"\n—Chandra Nalaar"
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6ff120a2-e2bb-42a2-bcb7-a48eb7a6d9b2.jpg?1783907889"
    }
}
