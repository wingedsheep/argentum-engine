package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeywordToCreatureGroup
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Adept Watershaper
 * {2}{W}
 * Creature — Merfolk Cleric
 * 3/4
 *
 * Other tapped creatures you control have indestructible.
 */
val AdeptWatershaper = card("Adept Watershaper") {
    manaCost = "{2}{W}"
    typeLine = "Creature — Merfolk Cleric"
    power = 3
    toughness = 4
    oracleText = "Other tapped creatures you control have indestructible."

    staticAbility {
        ability = GrantKeywordToCreatureGroup(
            keyword = Keyword.INDESTRUCTIBLE,
            filter = GroupFilter.OtherTappedCreaturesYouControl,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "3"
        artist = "Pauline Voss"
        flavorText = "\"If even the simplest land-dweller can divert the river with no more than a shovel, just imagine what I can do.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6e53f246-8347-4632-9d5b-4aeb12f7b762.jpg?1767951683"
        ruling("2025-11-17", "Because damage remains marked on creatures until the damage is removed as the turn ends, nonlethal damage dealt to a creature you control may become lethal if that creature becomes untapped or if Adept Watershaper leaves the battlefield during that turn.")
    }
}
