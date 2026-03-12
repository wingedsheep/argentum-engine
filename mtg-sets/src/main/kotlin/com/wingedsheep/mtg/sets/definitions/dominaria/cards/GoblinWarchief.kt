package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeywordToCreatureGroup
import com.wingedsheep.sdk.scripting.ReduceSpellCostBySubtype
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

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
        ability = ReduceSpellCostBySubtype("Goblin", 1)
    }

    staticAbility {
        ability = GrantKeywordToCreatureGroup(
            keyword = Keyword.HASTE,
            filter = GroupFilter(GameObjectFilter.Creature.youControl().withSubtype("Goblin"))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "130"
        artist = "Karl Kopinski"
        flavorText = "\"Not since the days of Pashalik Mons have the Rundvelt goblins been so united or effective.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5bac033c-dc4e-40a0-b103-4892e4b50249.jpg?1562736294"
    }
}
