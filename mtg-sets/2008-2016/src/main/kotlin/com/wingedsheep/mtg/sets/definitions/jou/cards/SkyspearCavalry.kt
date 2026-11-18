package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Skyspear Cavalry
 * {3}{W}{W}
 * Creature — Human Soldier
 * 2/2
 *
 * Flying
 * Double strike (This creature deals both first-strike and regular combat damage.)
 */
val SkyspearCavalry = card("Skyspear Cavalry") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "Double strike (This creature deals both first-strike and regular combat damage.)"

    keywords(Keyword.FLYING, Keyword.DOUBLE_STRIKE)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "26"
        artist = "Wayne Reynolds"
        flavorText = "Pray her spear reaches you first. Her griffin is less kind."
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c86ab26f-0959-476f-9f83-939e9138ca8d.jpg?1783939454"
    }
}
