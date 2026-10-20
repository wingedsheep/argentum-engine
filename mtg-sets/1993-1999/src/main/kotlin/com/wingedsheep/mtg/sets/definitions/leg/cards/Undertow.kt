package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SuppressLandwalkForGroup

/**
 * Undertow
 * {2}{U}
 * Enchantment
 *
 * Creatures with islandwalk can be blocked as though they didn't have islandwalk.
 */
val Undertow = card("Undertow") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Creatures with islandwalk can be blocked as though they didn't have islandwalk."

    staticAbility {
        ability = SuppressLandwalkForGroup(Keyword.ISLANDWALK)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "82"
        artist = "Randy Asplund-Faith"
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cf05e5c9-b7e4-4bd8-ab73-b54565710527.jpg?1783948070"
    }
}
