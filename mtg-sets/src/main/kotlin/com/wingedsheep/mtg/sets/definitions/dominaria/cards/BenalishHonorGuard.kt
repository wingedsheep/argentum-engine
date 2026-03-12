package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.StaticTarget
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Benalish Honor Guard
 * {1}{W}
 * Creature — Human Knight
 * 2/2
 * Benalish Honor Guard gets +1/+0 for each legendary creature you control.
 */
val BenalishHonorGuard = card("Benalish Honor Guard") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Human Knight"
    power = 2
    toughness = 2
    oracleText = "Benalish Honor Guard gets +1/+0 for each legendary creature you control."

    staticAbility {
        ability = GrantDynamicStatsEffect(
            target = StaticTarget.SourceCreature,
            powerBonus = DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Creature.legendary()),
            toughnessBonus = DynamicAmount.Fixed(0)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "5"
        artist = "Ryan Pancoast"
        flavorText = "\"The true measure of all heroes is not what they achieve, but who they inspire.\" —Triumph of Gerrard"
        imageUri = "https://cards.scryfall.io/normal/front/7/3/73a5ecf1-2063-4cb3-a4ab-a0601b28256a.jpg?1562737807"
    }
}
