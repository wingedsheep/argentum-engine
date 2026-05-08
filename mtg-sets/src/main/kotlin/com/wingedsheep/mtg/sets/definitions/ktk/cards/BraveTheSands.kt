package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanBlockAdditionalForCreatureGroup
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Brave the Sands
 * {1}{W}
 * Enchantment
 * Creatures you control have vigilance.
 * Each creature you control can block an additional creature each combat.
 */
val BraveTheSands = card("Brave the Sands") {
    manaCost = "{1}{W}"
    typeLine = "Enchantment"
    oracleText = "Creatures you control have vigilance.\nEach creature you control can block an additional creature each combat."

    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.VIGILANCE,
            filter = GroupFilter.AllCreaturesYouControl
        )
    }

    staticAbility {
        ability = CanBlockAdditionalForCreatureGroup(
            count = 1,
            filter = GroupFilter.AllCreaturesYouControl
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "5"
        artist = "Dave Kendall"
        flavorText = "Enduring the most desolate and dangerous conditions, Abzan sentries unfailingly guard the stronghold gates."
        imageUri = "https://cards.scryfall.io/normal/front/e/f/ef9c2b8a-148c-4393-b691-e333ad11c44e.jpg?1562795758"
    }
}
