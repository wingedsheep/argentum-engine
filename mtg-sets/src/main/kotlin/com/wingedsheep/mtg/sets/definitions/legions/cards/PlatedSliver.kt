package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStatsForCreatureGroup
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Plated Sliver
 * {W}
 * Creature — Sliver
 * 1/1
 * All Sliver creatures get +0/+1.
 */
val PlatedSliver = card("Plated Sliver") {
    manaCost = "{W}"
    typeLine = "Creature — Sliver"
    power = 1
    toughness = 1
    oracleText = "All Sliver creatures get +0/+1."

    staticAbility {
        ability = ModifyStatsForCreatureGroup(
            powerBonus = 0,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Sliver"))
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "19"
        artist = "Greg Staples"
        flavorText = "Overcoming extinction has only made the slivers more determined to live."
        imageUri = "https://cards.scryfall.io/normal/front/8/2/82846d31-4981-4ef1-85c3-703569146a84.jpg?1562921399"
    }
}
