package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SuppressLandwalkForGroup

/**
 * Quagmire
 * {2}{B}
 * Enchantment
 *
 * Creatures with swampwalk can be blocked as though they didn't have swampwalk.
 */
val Quagmire = card("Quagmire") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Creatures with swampwalk can be blocked as though they didn't have swampwalk."

    staticAbility {
        ability = SuppressLandwalkForGroup(Keyword.SWAMPWALK)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "115"
        artist = "Dan Frazier"
        imageUri = "https://cards.scryfall.io/normal/front/9/4/94e2aa9e-af6a-41c6-99a8-ca9335730ddb.jpg?1783948064"
    }
}
