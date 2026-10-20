package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SuppressLandwalkForGroup

/**
 * Great Wall
 * {2}{W}
 * Enchantment
 *
 * Creatures with plainswalk can be blocked as though they didn't have plainswalk.
 */
val GreatWall = card("Great Wall") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Creatures with plainswalk can be blocked as though they didn't have plainswalk."

    staticAbility {
        ability = SuppressLandwalkForGroup(Keyword.PLAINSWALK)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "17"
        artist = "Sandra Everingham"
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cd860a1d-aa17-4579-b9b1-d101d2416387.jpg?1783948084"
    }
}
