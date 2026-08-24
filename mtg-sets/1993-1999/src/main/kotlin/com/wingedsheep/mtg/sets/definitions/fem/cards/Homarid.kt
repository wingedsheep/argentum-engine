package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Homarid
 * {2}{U}
 * Creature — Homarid
 * 2/2
 * This creature enters with a tide counter on it.
 * At the beginning of your upkeep, put a tide counter on this creature.
 * As long as there is exactly one tide counter on this creature, it gets -1/-1.
 * As long as there are exactly three tide counters on this creature, it gets +1/+1.
 * Whenever there are four or more tide counters on this creature, remove all tide counters from it.
 *
 * The tide is a four-beat cycle: 1 → 1/1, 2 → 2/2, 3 → 3/3, then the fourth counter is shed and it
 * starts again from zero. Unlike every other stored counter in the set, what matters is the
 * *exact* count, so both statics compare for equality rather than a threshold — a creature with
 * three counters is not also "at least one".
 *
 * The reset is a state trigger (CR 603.8), not an upkeep trigger: it fires the moment the fourth
 * counter lands, however it got there.
 */
val Homarid = card("Homarid") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Homarid"
    oracleText = "This creature enters with a tide counter on it.\n" +
        "At the beginning of your upkeep, put a tide counter on this creature.\n" +
        "As long as there is exactly one tide counter on this creature, it gets -1/-1.\n" +
        "As long as there are exactly three tide counters on this creature, it gets +1/+1.\n" +
        "Whenever there are four or more tide counters on this creature, remove all tide counters from it."
    power = 2
    toughness = 2

    replacementEffect(
        EntersWithCounters(
            counterType = CounterTypeFilter.Named(Counters.TIDE),
            count = 1,
            selfOnly = true
        )
    )

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = Effects.AddCounters(Counters.TIDE, 1, EffectTarget.Self)
        description = "At the beginning of your upkeep, put a tide counter on this creature."
    }

    staticAbility {
        condition = Conditions.CompareAmounts(
            DynamicAmounts.countersOnSelf(CounterTypeFilter.Named(Counters.TIDE)),
            ComparisonOperator.EQ,
            DynamicAmount.Fixed(1),
        )
        ability = ModifyStats(-1, -1, GroupFilter.source())
    }

    staticAbility {
        condition = Conditions.CompareAmounts(
            DynamicAmounts.countersOnSelf(CounterTypeFilter.Named(Counters.TIDE)),
            ComparisonOperator.EQ,
            DynamicAmount.Fixed(3),
        )
        ability = ModifyStats(1, 1, GroupFilter.source())
    }

    stateTriggeredAbility {
        condition = Conditions.SourceCounterCountAtLeast(Counters.TIDE, 4)
        effect = Effects.RemoveAllCountersOfType(Counters.TIDE, EffectTarget.Self)
        description = "Whenever there are four or more tide counters on this creature, remove all tide counters from it."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "19a"
        artist = "Quinton Hoover"
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d6ffeab4-83b1-4414-ae72-e59a2354ea15.jpg?1783947913"
    }
}
