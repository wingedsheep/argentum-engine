package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Wayfaring Giant
 * {5}{W}
 * Creature — Giant
 * 1/3
 * Domain — This creature gets +1/+1 for each basic land type among lands you control.
 */
val WayfaringGiant = card("Wayfaring Giant") {
    manaCost = "{5}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Giant"
    power = 1
    toughness = 3
    oracleText = "Domain — This creature gets +1/+1 for each basic land type among lands you control."

    staticAbility {
        ability = GrantDynamicStatsEffect(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmounts.domain(),
            toughnessBonus = DynamicAmounts.domain()
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "44"
        artist = "Christopher Moeller"
        imageUri = "https://cards.scryfall.io/normal/front/5/7/57e45de5-0e8b-41d3-979b-ec5a29cac682.jpg?1562912879"
    }
}
