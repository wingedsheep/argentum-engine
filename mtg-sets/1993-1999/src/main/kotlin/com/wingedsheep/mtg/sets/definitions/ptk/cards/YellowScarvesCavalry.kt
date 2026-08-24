package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock

/**
 * Yellow Scarves Cavalry
 * {1}{R}
 * Creature — Human Soldier
 * 1/1
 * Horsemanship (This creature can't be blocked except by creatures with horsemanship.)
 * This creature can't block.
 */
val YellowScarvesCavalry = card("Yellow Scarves Cavalry") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 1
    oracleText =
        "Horsemanship (This creature can't be blocked except by creatures with horsemanship.)\n" +
        "This creature can't block."

    keywords(Keyword.HORSEMANSHIP)

    staticAbility {
        ability = CantBlock()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "125"
        artist = "Chen Weidong"
        imageUri = "https://cards.scryfall.io/normal/front/8/4/84460666-4f6b-422c-b20e-dc1651c66e15.jpg"
    }
}
