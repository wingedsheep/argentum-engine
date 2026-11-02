package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ajani, Wise Counselor
 * {3}{W}{W}
 * Legendary Planeswalker — Ajani
 * Loyalty: 5
 *
 * +2: You gain 1 life for each creature you control.
 * −3: Creatures you control get +2/+2 until end of turn.
 * −9: Put X +1/+1 counters on target creature, where X is your life total.
 *
 * The −3 locks in the affected set at resolution (iterating the group), and the −9 reads the life
 * total as it resolves — both per the 2018-07-13 rulings.
 */
val AjaniWiseCounselor = card("Ajani, Wise Counselor") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Planeswalker — Ajani"
    startingLoyalty = 5
    oracleText = "+2: You gain 1 life for each creature you control.\n" +
        "−3: Creatures you control get +2/+2 until end of turn.\n" +
        "−9: Put X +1/+1 counters on target creature, where X is your life total."

    loyaltyAbility(+2) {
        effect = Effects.GainLife(DynamicAmounts.creaturesYouControl())
    }

    loyaltyAbility(-3) {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.ModifyStats(2, 2, EffectTarget.IterationEntity)
        )
    }

    loyaltyAbility(-9) {
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddDynamicCounters(
            CounterType.PLUS_ONE_PLUS_ONE,
            DynamicAmounts.yourLifeTotal(),
            creature,
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "281"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b4a22ecc-3a5c-44a1-bb52-bbeffa2dbf17.jpg?1783934494"
        ruling(
            "2018-07-13",
            "Ajani's second ability affects only creatures you control at the time it resolves. " +
                "Creatures you begin to control later in the turn won't get +2/+2."
        )
        ruling(
            "2018-07-13",
            "The value of X is determined only as Ajani's third ability resolves. If your life total " +
                "changes later, the creature won't gain or lose +1/+1 counters."
        )
    }
}
