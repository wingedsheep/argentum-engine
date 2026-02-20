package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.ModifyStatsForCreatureGroup
import com.wingedsheep.sdk.scripting.ReduceSpellCostBySubtype

/**
 * Undead Warchief
 * {2}{B}{B}
 * Creature — Zombie
 * 1/1
 * Zombie spells you cast cost {1} less to cast.
 * Zombie creatures you control get +2/+1.
 */
val UndeadWarchief = card("Undead Warchief") {
    manaCost = "{2}{B}{B}"
    typeLine = "Creature — Zombie"
    power = 1
    toughness = 1
    oracleText = "Zombie spells you cast cost {1} less to cast.\nZombie creatures you control get +2/+1."

    staticAbility {
        ability = ReduceSpellCostBySubtype(
            subtype = "Zombie",
            amount = 1
        )
    }

    staticAbility {
        ability = ModifyStatsForCreatureGroup(
            powerBonus = 2,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Zombie").youControl())
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "77"
        artist = "Thomas M. Baxa"
        flavorText = "It wields the banners of eighteen conquered armies."
        imageUri = "https://cards.scryfall.io/large/front/e/6/e6b3bcfe-be82-458b-ba59-ecb84436d747.jpg?1562536237"
    }
}
