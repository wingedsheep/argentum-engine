package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Bring Low
 * {3}{R}
 * Instant
 * Bring Low deals 3 damage to target creature. If that creature has a +1/+1 counter on it,
 * Bring Low deals 5 damage to it instead.
 */
val BringLow = card("Bring Low") {
    manaCost = "{3}{R}"
    typeLine = "Instant"
    oracleText = "Bring Low deals 3 damage to target creature. If that creature has a +1/+1 counter on it, Bring Low deals 5 damage to it instead."

    spell {
        effect = Effects.DealDamage(
            amount = DynamicAmount.Conditional(
                condition = Conditions.TargetHasCounter(CounterTypeFilter.PlusOnePlusOne),
                ifTrue = DynamicAmount.Fixed(5),
                ifFalse = DynamicAmount.Fixed(3)
            ),
            target = EffectTarget.ContextTarget(0)
        )
        target = Targets.Creature
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "103"
        artist = "Slawomir Maniak"
        flavorText = "\"People are often humbled by the elements. But the elements, too, can be humbled.\"\n—Surrak, khan of the Temur"
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9ba5e7bf-2ad8-4061-937a-ef1e9b63da3d.jpg?1562790998"
    }
}
