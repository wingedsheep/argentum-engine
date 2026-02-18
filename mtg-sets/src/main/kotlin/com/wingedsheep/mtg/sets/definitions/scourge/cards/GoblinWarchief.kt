package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeywordToCreatureGroup
import com.wingedsheep.sdk.scripting.GroupFilter
import com.wingedsheep.sdk.scripting.ReduceSpellCostBySubtype

/**
 * Goblin Warchief
 * {1}{R}{R}
 * Creature — Goblin Warrior
 * 2/2
 * Goblin spells you cast cost {1} less to cast.
 * Goblins you control have haste.
 */
val GoblinWarchief = card("Goblin Warchief") {
    manaCost = "{1}{R}{R}"
    typeLine = "Creature — Goblin Warrior"
    power = 2
    toughness = 2
    oracleText = "Goblin spells you cast cost {1} less to cast.\nGoblins you control have haste."

    staticAbility {
        ability = ReduceSpellCostBySubtype(
            subtype = "Goblin",
            amount = 1
        )
    }

    staticAbility {
        ability = GrantKeywordToCreatureGroup(
            keyword = Keyword.HASTE,
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Goblin"))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "97"
        artist = "Tim Hildebrandt"
        flavorText = "They don't need a reason to fight. They just need a leader."
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5e6d56cb-660a-4571-a493-98f4cd1e14ea.jpg?1562529590"
    }
}
