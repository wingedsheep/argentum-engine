package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Scourge of Geier Reach
 * {3}{R}{R}
 * Creature — Elemental
 * 3/3
 * This creature gets +1/+1 for each creature your opponents control.
 */
val ScourgeOfGeierReach = card("Scourge of Geier Reach") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental"
    oracleText = "This creature gets +1/+1 for each creature your opponents control."
    power = 3
    toughness = 3
    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmounts.battlefield(Player.EachOpponent, GameObjectFilter.Creature).count(),
            toughnessBonus = DynamicAmounts.battlefield(Player.EachOpponent, GameObjectFilter.Creature).count(),
        )
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "162"
        artist = "Jung Park"
        flavorText = "Stensian villagers mourned the loss of human life. Thraben vampire slayers mourned the loss of living wood."
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e0c25932-96e7-4ae5-b544-8780f92d0be7.jpg"
    }
}
