package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Frilled Cave-Wurm — {3}{U}
 * Creature — Salamander Wurm
 * 2/5
 * Descend 4 — This creature gets +2/+0 as long as there are four or more permanent cards
 * in your graveyard.
 */
val FrilledCaveWurm = card("Frilled Cave-Wurm") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Salamander Wurm"
    oracleText = "Descend 4 — This creature gets +2/+0 as long as there are four or more permanent cards in your graveyard."
    power = 2
    toughness = 5

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(powerBonus = 2, toughnessBonus = 0, filter = GroupFilter.source()),
            condition = Conditions.CardsInGraveyardMatchingAtLeast(4, GameObjectFilter.Permanent)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "57"
        artist = "Aaron Miller"
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6f65e1bc-fade-4fdf-a1fd-de068bff9e4c.jpg?1782694563"
    }
}
