package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ichor Synthesizer
 * {1}{U}
 * Creature — Phyrexian Wizard
 * 1/3
 *
 * Whenever you cast a noncreature spell, put an oil counter on this creature.
 * As long as this creature has four or more oil counters on it, it gets +2/+0 and can't be blocked.
 */
val IchorSynthesizer = card("Ichor Synthesizer") {
    manaCost = "{1}{U}"
    typeLine = "Creature — Phyrexian Wizard"
    power = 1
    toughness = 3
    oracleText = "Whenever you cast a noncreature spell, put an oil counter on this creature.\n" +
        "As long as this creature has four or more oil counters on it, it gets +2/+0 and can't be blocked."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature)
        effect = Effects.AddCounters(CounterType.OIL, 1, EffectTarget.Self)
    }

    staticAbility {
        condition = Conditions.SourceCounterCountAtLeast(CounterType.OIL, 4)
        ability = ModifyStats(+2, +0, Filters.Self)
    }
    staticAbility {
        condition = Conditions.SourceCounterCountAtLeast(CounterType.OIL, 4)
        ability = CantBeBlocked()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "55"
        artist = "Sam Wolfe Connelly"
        flavorText = "\"I've isolated the chirality of the glistening oil, so there will be no wasted precursors.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f516ee6-8f7b-40b2-82e5-9c4ea3e0a355.jpg?1783918063"
    }
}
