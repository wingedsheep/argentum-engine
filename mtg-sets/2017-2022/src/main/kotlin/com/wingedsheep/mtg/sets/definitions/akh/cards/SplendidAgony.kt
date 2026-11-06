package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Splendid Agony
 * {2}{B}
 * Instant
 * Distribute two -1/-1 counters among one or two target creatures.
 *
 * Abzan Charm's distribute mode with -1/-1 counters: the division is made as the spell is cast,
 * each target gets at least one counter, and a counter assigned to a target that became illegal
 * is lost rather than moved to the remaining target.
 */
val SplendidAgony = card("Splendid Agony") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Distribute two -1/-1 counters among one or two target creatures."

    spell {
        targets(TargetFilter.Creature, count = 2, minCount = 1)
        effect = Effects.DistributeCountersAmongTargets(
            totalCounters = 2,
            counterType = CounterType.MINUS_ONE_MINUS_ONE
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "109"
        artist = "Clint Cearley"
        flavorText = "\"Wherever you are in the city, you can feel the presence of the God-Pharaoh. Take comfort in that.\"\n—Ukhat, vizier of initiation"
        imageUri = "https://cards.scryfall.io/normal/front/5/8/58d38abd-116d-4015-91e4-3fc267ad099d.jpg?1783936498"
        ruling("2017-04-18", "You choose how the counters will be distributed as you cast Splendid Agony. Each target creature must be assigned at least one counter.")
        ruling("2017-04-18", "If one of two target creatures becomes an illegal target in response to Splendid Agony, the -1/-1 counter that would have been put on that creature is lost. It can’t be put on the legal target.")
    }
}
