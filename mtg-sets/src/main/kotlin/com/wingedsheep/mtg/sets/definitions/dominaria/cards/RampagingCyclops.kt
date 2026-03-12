package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.StaticTarget
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Rampaging Cyclops
 * {3}{R}
 * Creature — Cyclops
 * 4/4
 * Rampaging Cyclops gets -2/-0 as long as two or more creatures are blocking it.
 */
val RampagingCyclops = card("Rampaging Cyclops") {
    manaCost = "{3}{R}"
    typeLine = "Creature — Cyclops"
    power = 4
    toughness = 4
    oracleText = "Rampaging Cyclops gets -2/-0 as long as two or more creatures are blocking it."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantDynamicStatsEffect(
                target = StaticTarget.SourceCreature,
                powerBonus = DynamicAmount.Fixed(-2),
                toughnessBonus = DynamicAmount.Fixed(0)
            ),
            condition = Compare(
                DynamicAmount.NumberOfBlockers,
                ComparisonOperator.GTE,
                DynamicAmount.Fixed(2)
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "139"
        artist = "Tomasz Jedruszek"
        flavorText = "\"Keldon warriors take advantage of the cyclops's inability to focus on more than one thing at a time.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7fdac0c6-bf72-4a9b-9fad-ae7bc2632a4a.jpg?1562738510"
    }
}
