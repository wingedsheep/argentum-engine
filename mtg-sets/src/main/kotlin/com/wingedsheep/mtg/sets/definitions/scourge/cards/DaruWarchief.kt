package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GroupFilter
import com.wingedsheep.sdk.scripting.ModifyStatsForCreatureGroup
import com.wingedsheep.sdk.scripting.ReduceSpellCostBySubtype

/**
 * Daru Warchief
 * {2}{W}{W}
 * Creature — Human Soldier
 * 1/1
 * Soldier spells you cast cost {1} less to cast.
 * Soldier creatures you control get +1/+2.
 */
val DaruWarchief = card("Daru Warchief") {
    manaCost = "{2}{W}{W}"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 1
    oracleText = "Soldier spells you cast cost {1} less to cast.\nSoldier creatures you control get +1/+2."

    staticAbility {
        ability = ReduceSpellCostBySubtype(
            subtype = "Soldier",
            amount = 1
        )
    }

    staticAbility {
        ability = ModifyStatsForCreatureGroup(
            powerBonus = 1,
            toughnessBonus = 2,
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Soldier"))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "6"
        artist = "Tim Hildebrandt"
        flavorText = "Within the complex Daru hierarchy, the weights of duty and of rank are equal."
        imageUri = "https://cards.scryfall.io/large/front/2/6/2630d3b5-8f3a-4aad-a45e-22a7979429f3.jpg?1562526606"
    }
}
