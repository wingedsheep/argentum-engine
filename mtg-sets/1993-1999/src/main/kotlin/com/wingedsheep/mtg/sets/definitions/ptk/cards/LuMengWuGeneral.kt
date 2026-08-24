package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Lu Meng, Wu General
 * {3}{U}{U}
 * Legendary Creature — Human Soldier
 */
val LuMengWuGeneral = card("Lu Meng, Wu General") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Soldier"
    power = 4
    toughness = 4
    oracleText = "Horsemanship (This creature can't be blocked except by creatures with horsemanship.)"

    keywords(Keyword.HORSEMANSHIP)

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "46"
        artist = "Gao Yan"
        flavorText = "As the Wu chief commander, Lu Meng conquered Shu-held Jingzhou in 219 by disguising soldiers as merchants on boats filled with hiding troops."
        imageUri = "https://cards.scryfall.io/normal/front/7/7/77a8b376-eda5-4bf9-9093-9518c09e50e8.jpg"
    }
}
