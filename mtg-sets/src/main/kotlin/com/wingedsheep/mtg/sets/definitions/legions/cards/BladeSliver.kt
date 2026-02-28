package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStatsForCreatureGroup
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Blade Sliver
 * {2}{R}
 * Creature — Sliver
 * 2/2
 * All Sliver creatures get +1/+0.
 */
val BladeSliver = card("Blade Sliver") {
    manaCost = "{2}{R}"
    typeLine = "Creature — Sliver"
    power = 2
    toughness = 2
    oracleText = "All Sliver creatures get +1/+0."

    staticAbility {
        ability = ModifyStatsForCreatureGroup(
            powerBonus = 1,
            toughnessBonus = 0,
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Sliver"))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "88"
        artist = "David Martin"
        flavorText = "After breaking free from the Riptide Project, the slivers quickly adapted to life on Otaria—much to the dismay of life on Otaria."
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a8d6f7a6-7b6a-44f4-be04-7c02806b9f09.jpg?1562929059"
    }
}
