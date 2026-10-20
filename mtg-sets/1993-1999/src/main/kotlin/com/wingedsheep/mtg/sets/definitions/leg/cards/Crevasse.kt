package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SuppressLandwalkForGroup

/**
 * Crevasse
 * {2}{R}
 * Enchantment
 *
 * Creatures with mountainwalk can be blocked as though they didn't have mountainwalk.
 */
val Crevasse = card("Crevasse") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Creatures with mountainwalk can be blocked as though they didn't have mountainwalk."

    staticAbility {
        ability = SuppressLandwalkForGroup(Keyword.MOUNTAINWALK)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "138"
        artist = "Rob Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a432d6ae-a17f-484b-ad55-4b4b6674ba8d.jpg?1783948059"
    }
}
