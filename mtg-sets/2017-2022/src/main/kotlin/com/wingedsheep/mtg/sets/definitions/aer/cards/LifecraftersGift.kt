package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lifecrafter's Gift
 * {3}{G}
 * Instant
 * Put a +1/+1 counter on target creature, then put a +1/+1 counter on each creature you control
 * with a +1/+1 counter on it.
 *
 * The group is read after the first counter lands, so a target you control gets two.
 */
val LifecraftersGift = card("Lifecrafter's Gift") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Put a +1/+1 counter on target creature, then put a +1/+1 counter on each creature you control with a +1/+1 counter on it."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
            Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Creature.withCounter(CounterType.PLUS_ONE_PLUS_ONE).youControl()),
                Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "114"
        artist = "Winona Nelson"
        flavorText = "The best engineers recognize when a problem has already been solved."
        imageUri = "https://cards.scryfall.io/normal/front/6/7/6759f13b-e755-495d-87df-a685455cdf33.jpg"
        ruling("2020-11-10", "Each +1/+1 counter put on the target creature is a separate event. Replacement effects may affect each of these counters being put on the creature.")
        ruling("2020-11-10", "If the target creature is an illegal target by the time Lifecrafter's Gift tries to resolve, the spell doesn't resolve. No creatures receive +1/+1 counters.")
        ruling("2020-11-10", "If there is a +1/+1 counter on a noncreature permanent you control, such as a Vehicle that isn't crewed, it won't get another one from Lifecrafter's Gift.")
        ruling("2020-11-10", "The state-based action that removes matching +1/+1 and -1/-1 counters won't check until after Lifecrafter's Gift finishes resolving. It's possible put a +1/+1 counter on a creature with a -1/-1 counter on it and then give it a second +1/+1 counter.")
    }
}
