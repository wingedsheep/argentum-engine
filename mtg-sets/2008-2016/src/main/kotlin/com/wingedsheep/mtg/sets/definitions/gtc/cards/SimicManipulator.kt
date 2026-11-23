package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.evolve
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Simic Manipulator — Gatecrash #50
 * {1}{U}{U} · Creature — Mutant Wizard · 0/1
 *
 * Evolve
 * {T}, Remove one or more +1/+1 counters from this creature: Gain control of target creature with
 * power less than or equal to the number of +1/+1 counters removed this way.
 *
 * The removal is Cruel Sadist's self-scoped variable cost: the number removed is the ability's X,
 * announced before targets (CR 601.2b–c), and "one or more" is `minimumXValue = 1`. The target's
 * power cap reads that X when the target is chosen and again on resolution (2013-01-24 ruling).
 */
val SimicManipulator = card("Simic Manipulator") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Mutant Wizard"
    oracleText = "Evolve (Whenever a creature you control enters, if that creature has greater power or " +
        "toughness than this creature, put a +1/+1 counter on this creature.)\n" +
        "{T}, Remove one or more +1/+1 counters from this creature: Gain control of target creature with " +
        "power less than or equal to the number of +1/+1 counters removed this way."
    power = 0
    toughness = 1

    evolve()

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.RemoveXCounters(counterType = CounterType.PLUS_ONE_PLUS_ONE, self = true),
        )
        minimumXValue = 1
        val creature = target(
            TargetFilter(GameObjectFilter.Creature.powerAtMostDynamic(DynamicAmounts.xValue()))
        )
        effect = Effects.GainControl(creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "50"
        artist = "Maciej Kuciara"
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e3dff9e6-5e0c-4e5b-8184-f0ae9cf347b3.jpg?1783940134"
        ruling(
            "2013-01-24",
            "The power of the target creature is checked both as you target it and as the ability resolves. " +
                "If the power of the target creature when the ability resolves is greater than the number of " +
                "+1/+1 counters removed from Simic Manipulator, the ability won't resolve and none of its " +
                "effects will happen. You won't gain control of any creature, but the counters removed as a " +
                "cost remain removed.",
        )
    }
}
