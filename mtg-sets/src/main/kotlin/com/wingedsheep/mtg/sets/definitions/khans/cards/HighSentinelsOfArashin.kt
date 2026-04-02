package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.StaticTarget
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * High Sentinels of Arashin
 * {3}{W}
 * Creature — Bird Soldier
 * 3/4
 * Flying
 * High Sentinels of Arashin gets +1/+1 for each other creature you control with a +1/+1 counter on it.
 * {3}{W}: Put a +1/+1 counter on target creature.
 */
val HighSentinelsOfArashin = card("High Sentinels of Arashin") {
    manaCost = "{3}{W}"
    typeLine = "Creature — Bird Soldier"
    power = 3
    toughness = 4
    oracleText = "Flying\nHigh Sentinels of Arashin gets +1/+1 for each other creature you control with a +1/+1 counter on it.\n{3}{W}: Put a +1/+1 counter on target creature."

    keywords(Keyword.FLYING)

    // Gets +1/+1 for each other creature you control with a +1/+1 counter on it
    staticAbility {
        ability = GrantDynamicStatsEffect(
            target = StaticTarget.SourceCreature,
            powerBonus = DynamicAmount.AggregateBattlefield(
                player = Player.You,
                filter = GameObjectFilter.Creature.withCounter(Counters.PLUS_ONE_PLUS_ONE),
                excludeSelf = true
            ),
            toughnessBonus = DynamicAmount.AggregateBattlefield(
                player = Player.You,
                filter = GameObjectFilter.Creature.withCounter(Counters.PLUS_ONE_PLUS_ONE),
                excludeSelf = true
            )
        )
    }

    // {3}{W}: Put a +1/+1 counter on target creature
    activatedAbility {
        cost = Costs.Mana("{3}{W}")
        val t = target("target creature", Targets.Creature)
        effect = AddCountersEffect(
            counterType = Counters.PLUS_ONE_PLUS_ONE,
            count = 1,
            target = t
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "13"
        artist = "James Ryman"
        imageUri = "https://cards.scryfall.io/normal/front/d/b/db5f4bab-f918-4b42-b82c-cfcf5ff0c58a.jpg?1562794547"
    }
}
