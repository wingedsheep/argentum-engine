package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Lord of Atlantis
 * {U}{U}
 * Creature — Merfolk
 * 2/2
 * Other Merfolk get +1/+1 and have islandwalk.
 */
val LordOfAtlantis = card("Lord of Atlantis") {
    manaCost = "{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk"
    oracleText = "Other Merfolk get +1/+1 and have islandwalk. (They can't be blocked as long as defending player controls an Island.)"
    power = 2
    toughness = 2
    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.MERFOLK), excludeSelf = true)
        )
    }
    staticAbility {
        ability = GrantKeyword(
            Keyword.ISLANDWALK,
            GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.MERFOLK), excludeSelf = true)
        )
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "62"
        artist = "Melissa A. Benson"
        flavorText = "A master of tactics, the Lord of Atlantis makes his people bold in battle merely by arriving to lead them."
        imageUri = "https://cards.scryfall.io/normal/front/2/1/210c4a90-fc7a-4c76-aeaa-20a005e45386.jpg"
    }
}
