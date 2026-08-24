package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Barbarian General
 * {4}{R}
 * Creature — Human Barbarian Soldier
 * 3/2
 *
 * Horsemanship (This creature can't be blocked except by creatures with horsemanship.)
 */
val BarbarianGeneral = card("Barbarian General") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Barbarian Soldier"
    power = 3
    toughness = 2
    oracleText = "Horsemanship (This creature can't be blocked except by creatures with horsemanship.)"

    keywords(Keyword.HORSEMANSHIP)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "100"
        artist = "Kuang Sheng"
        flavorText = "\"Barbarian tribes with their rulers are inferior to Chinese states without them.\"\n—Confucius, *The Analects* (trans. Lau)"
        imageUri = "https://cards.scryfall.io/normal/front/0/6/060da652-03c7-45e3-ad83-f0a9fa9d1049.jpg"
    }
}
