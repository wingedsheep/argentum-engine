package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SuppressLandwalkForGroup

/**
 * Gosta Dirk
 * {3}{W}{W}{U}{U}
 * Legendary Creature — Human Warrior
 * 4/4
 * First strike
 * Creatures with islandwalk can be blocked as though they didn't have islandwalk.
 */
val GostaDirk = card("Gosta Dirk") {
    manaCost = "{3}{W}{W}{U}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Human Warrior"
    oracleText = "First strike\n" +
        "Creatures with islandwalk can be blocked as though they didn't have islandwalk."
    power = 4
    toughness = 4

    keywords(Keyword.FIRST_STRIKE)

    staticAbility {
        ability = SuppressLandwalkForGroup(Keyword.ISLANDWALK)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "227"
        artist = "Richard Thomas"
        imageUri = "https://cards.scryfall.io/normal/front/9/2/92ef316b-dd22-40d1-82e8-8890976684c0.jpg?1783948040"
    }
}
