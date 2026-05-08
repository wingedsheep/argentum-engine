package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Tempest Djinn
 * {U}{U}{U}
 * Creature — Djinn
 * 0/4
 * Flying
 * Tempest Djinn gets +1/+0 for each basic Island you control.
 */
val TempestDjinn = card("Tempest Djinn") {
    manaCost = "{U}{U}{U}"
    typeLine = "Creature — Djinn"
    power = 0
    toughness = 4
    oracleText = "Flying\nTempest Djinn gets +1/+0 for each basic Island you control."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = GrantDynamicStatsEffect(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.BasicLand.withSubtype("Island")),
            toughnessBonus = DynamicAmount.Fixed(0)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "68"
        artist = "Zezhou Chen"
        flavorText = "The first to arrive on Dominaria from their distant home, the marids are the oldest tribe of djinn and the most respected by storm and sea."
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3acc883b-3aea-4d0b-ae0f-00d4a08c47c1.jpg?1562734234"
    }
}
