package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SuppressLandwalkForGroup

/**
 * Deadfall
 * {2}{G}
 * Enchantment
 *
 * Creatures with forestwalk can be blocked as though they didn't have forestwalk.
 */
val Deadfall = card("Deadfall") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Creatures with forestwalk can be blocked as though they didn't have forestwalk."

    staticAbility {
        ability = SuppressLandwalkForGroup(Keyword.FORESTWALK)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "181"
        artist = "NéNé Thomas"
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d78f0fc-3ab2-46ee-b5a9-55ae97d08c1a.jpg?1783948049"
    }
}
