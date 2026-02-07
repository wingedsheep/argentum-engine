package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GroupFilter
import com.wingedsheep.sdk.scripting.ModifyStatsForCreatureGroup

/**
 * Aven Brigadier
 * {3}{W}{W}{W}
 * Creature — Bird Soldier
 * 3/5
 * Flying
 * Other Bird creatures get +1/+1.
 * Other Soldier creatures get +1/+1.
 */
val AvenBrigadier = card("Aven Brigadier") {
    manaCost = "{3}{W}{W}{W}"
    typeLine = "Creature — Bird Soldier"
    power = 3
    toughness = 5

    keywords(Keyword.FLYING)

    staticAbility {
        ability = ModifyStatsForCreatureGroup(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Bird"), excludeSelf = true)
        )
    }

    staticAbility {
        ability = ModifyStatsForCreatureGroup(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Soldier"), excludeSelf = true)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "7"
        artist = "Greg Staples"
        flavorText = "He represents what little pride the Order has left."
        imageUri = "https://cards.scryfall.io/normal/front/d/a/da24ef56-8d54-4146-97e9-4abded807545.jpg?1562946977"
    }
}
