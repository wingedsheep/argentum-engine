package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Rising of the Day
 * {2}{R}
 * Enchantment
 *
 * Creatures you control have haste.
 * Legendary creatures you control get +1/+0.
 */
val RisingOfTheDay = card("Rising of the Day") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Creatures you control have haste.\nLegendary creatures you control get +1/+0."

    // Creatures you control have haste.
    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.HASTE,
            filter = GroupFilter(GameObjectFilter.Creature.youControl())
        )
    }

    // Legendary creatures you control get +1/+0.
    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 0,
            filter = GroupFilter(GameObjectFilter.Creature.legendary().youControl())
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "145"
        artist = "Wei Guan"
        flavorText = "Over the low hills the horns were sounding. Hastening down the long slopes were a thousand men on foot; their swords were in their hands."
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d4fbcac6-8ac9-44a8-9d1a-03d0799ac253.jpg?1686969140"
    }
}
