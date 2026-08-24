package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock

/**
 * Yellow Scarves Troops
 * {1}{R}
 * Creature — Human Soldier
 * 2/2
 * This creature can't block.
 */
val YellowScarvesTroops = card("Yellow Scarves Troops") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "This creature can't block."

    staticAbility {
        ability = CantBlock()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "127"
        artist = "Chen Weidong"
        flavorText = "Over 500,000 commoners followed Zhang Jue, General of Heaven, in his attempt to overthrow the corrupt Han dynasty."
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3ca36a8d-ca44-485e-a219-85814f160c4d.jpg"
    }
}
