package com.wingedsheep.mtg.sets.definitions.lorwyn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CreatureGroupFilter
import com.wingedsheep.sdk.scripting.GrantKeywordToCreatureGroup

/**
 * Adept Watershaper
 * {2}{W}
 * Creature — Merfolk Cleric
 * 3/4
 * Other tapped creatures you control have indestructible.
 */
val AdeptWatershaper = card("Adept Watershaper") {
    manaCost = "{2}{W}"
    typeLine = "Creature — Merfolk Cleric"
    power = 3
    toughness = 4

    staticAbility {
        ability = GrantKeywordToCreatureGroup(
            keyword = Keyword.INDESTRUCTIBLE,
            filter = CreatureGroupFilter.OtherTappedYouControl
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "3"
        artist = "Pauline Voss"
    }
}
