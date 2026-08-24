package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Mountain Bandit
 * {R}
 * Creature — Human Soldier Rogue
 * 1/1
 * Haste
 */
val MountainBandit = card("Mountain Bandit") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Soldier Rogue"
    power = 1
    toughness = 1
    oracleText = "Haste"

    keywords(Keyword.HASTE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "117"
        artist = "Xu Xiaoming"
        flavorText = "Penniless and far from home, many former Yellow Scarves and other soldiers became bandits to survive."
        imageUri = "https://cards.scryfall.io/normal/front/3/4/34fd541d-9956-4595-9527-a83db4c5f74f.jpg"
    }
}
