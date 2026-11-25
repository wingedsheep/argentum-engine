package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Master of the Pearl Trident
 * {U}{U}
 * Creature — Merfolk
 * 2/2
 * Other Merfolk creatures you control get +1/+1 and have islandwalk.
 */
val MasterOfThePearlTrident = card("Master of the Pearl Trident") {
    manaCost = "{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk"
    oracleText = "Other Merfolk creatures you control get +1/+1 and have islandwalk. " +
        "(They can't be blocked as long as defending player controls an Island.)"
    power = 2
    toughness = 2

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature.withSubtype(Subtype.MERFOLK).youControl(),
                excludeSelf = true
            )
        )
    }
    staticAbility {
        ability = GrantKeyword(
            Keyword.ISLANDWALK,
            GroupFilter(
                GameObjectFilter.Creature.withSubtype(Subtype.MERFOLK).youControl(),
                excludeSelf = true
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "59"
        artist = "Ryan Pancoast"
        flavorText = "\"Let the land dwellers know the coast is no longer the border between our realms. A new age of empire has begun.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e7decbd3-c754-451c-8d63-4f31f81412d2.jpg"
    }
}
