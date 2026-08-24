package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Lu Bu, Master-at-Arms
 * {5}{R}
 * Legendary Creature — Human Soldier Warrior
 * 4/3
 * Haste; horsemanship (This creature can't be blocked except by creatures with horsemanship.)
 *
 * Two parameterless keywords printed on one semicolon-joined line.
 */
val LuBuMasterAtArms = card("Lu Bu, Master-at-Arms") {
    manaCost = "{5}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Soldier Warrior"
    power = 4
    toughness = 3
    oracleText = "Haste; horsemanship (This creature can't be blocked except by creatures with horsemanship.)"

    keywords(Keyword.HASTE, Keyword.HORSEMANSHIP)

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "115"
        artist = "Gao Jianzhang"
        flavorText = "\"Dong Zhuo's man, Lu Bu, warrior without peer, / Far surpassed the champions of his sphere.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/7/07d60c27-8625-48eb-a3f0-1e26d6930ae7.jpg"
    }
}
