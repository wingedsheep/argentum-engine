package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pelt Collector
 * {G}
 * Creature — Elf Warrior
 * 1/1
 * Whenever another creature you control enters or dies, if that creature's power is greater than
 * this creature's, put a +1/+1 counter on this creature.
 * As long as this creature has three or more +1/+1 counters on it, it has trample.
 *
 * "Enters or dies" is two triggers sharing one intervening-if (no single event is both, as on Mogg
 * War Marshal). The comparison is evolve's power half: `powerOf(TriggeringEntity)` reads the
 * entering creature's projected power, and a creature that died (or left before resolution) by its
 * last-known power, per the rulings.
 */
val PeltCollector = card("Pelt Collector") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Warrior"
    power = 1
    toughness = 1
    oracleText = "Whenever another creature you control enters or dies, if that creature's power is greater than " +
        "this creature's, put a +1/+1 counter on this creature.\n" +
        "As long as this creature has three or more +1/+1 counters on it, it has trample."

    val triggeringCreatureIsBigger = Conditions.CompareAmounts(
        DynamicAmounts.powerOf(EffectTarget.TriggeringEntity),
        ComparisonOperator.GT,
        DynamicAmounts.powerOf(EffectTarget.Self),
    )

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
        interveningIf = triggeringCreatureIsBigger
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).dies()
        interveningIf = triggeringCreatureIsBigger
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    staticAbility {
        condition = Conditions.SourceCounterCountAtLeast(CounterType.PLUS_ONE_PLUS_ONE, 3)
        ability = GrantKeyword(Keyword.TRAMPLE, Filters.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "141"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/b/e/bee125cb-12b9-4f78-8e1d-6018d9c52275.jpg?1783934147"
        ruling(
            "2018-10-05",
            "To determine if Pelt Collector's first ability triggers when a creature dies, use its power as it last existed on the battlefield."
        )
        ruling(
            "2018-10-05",
            "If a creature with power less than or equal to Pelt Collector's power enters the battlefield or dies, Pelt Collector's first ability doesn't trigger at all. You can't try to raise that creature's power or lower Pelt Collector's to get a counter."
        )
        ruling(
            "2018-10-05",
            "As Pelt Collector's first ability resolves, if the entering creature's power is no longer greater than Pelt Collector's power, or if Pelt Collector's power has been raised to greater than or equal to the power of the creature that died, Pelt Collector doesn't get a +1/+1 counter. Notably, this means that if Pelt Collector is still a 1/1 creature and two 2/2 creatures die, it only gets one +1/+1 counter as the first triggered ability resolves, and it will be too big to get a counter as the second triggered ability resolves."
        )
    }
}
