package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AttackerCountLimit
import com.wingedsheep.sdk.scripting.BlockerCountLimit

/**
 * Caverns of Despair
 * {2}{R}{R}
 * World Enchantment
 *
 * No more than two creatures can attack each combat.
 * No more than two creatures can block each combat.
 */
val CavernsOfDespair = card("Caverns of Despair") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "World Enchantment"
    oracleText = "No more than two creatures can attack each combat.\n" +
        "No more than two creatures can block each combat."

    staticAbility {
        ability = AttackerCountLimit(maxAttackers = 2)
    }

    staticAbility {
        ability = BlockerCountLimit(maxBlockers = 2)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "136"
        artist = "Harold McNeill"
        imageUri = "https://cards.scryfall.io/normal/front/2/0/209f7479-b3a0-4c27-9602-78babb8d2e99.jpg?1783948059"
    }
}
