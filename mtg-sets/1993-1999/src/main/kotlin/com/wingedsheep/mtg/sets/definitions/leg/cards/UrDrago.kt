package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SuppressLandwalkForGroup

/**
 * Ur-Drago
 * {3}{U}{U}{B}{B}
 * Legendary Creature — Elemental
 * 4/4
 *
 * First strike
 * Creatures with swampwalk can be blocked as though they didn't have swampwalk.
 */
val UrDrago = card("Ur-Drago") {
    manaCost = "{3}{U}{U}{B}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Elemental"
    power = 4
    toughness = 4
    oracleText = "First strike\nCreatures with swampwalk can be blocked as though they didn't have swampwalk."

    keywords(Keyword.FIRST_STRIKE)

    staticAbility {
        ability = SuppressLandwalkForGroup(Keyword.SWAMPWALK)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "268"
        artist = "Christopher Rush"
        imageUri = "https://cards.scryfall.io/normal/front/8/1/81a40f34-fc26-4d05-9c52-6ffbf1766a3b.jpg?1783948030"
    }
}
