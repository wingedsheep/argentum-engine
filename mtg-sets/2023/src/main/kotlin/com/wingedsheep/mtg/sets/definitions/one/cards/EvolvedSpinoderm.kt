package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Evolved Spinoderm
 * {2}{G}{G}
 * Creature — Phyrexian Beast
 * 5/5
 *
 * This creature enters with four oil counters on it.
 * This creature has trample as long as it has two or fewer oil counters on it. Otherwise, it has hexproof.
 * At the beginning of your upkeep, remove an oil counter from this creature. Then if it has no oil
 * counters on it, sacrifice it.
 *
 * "Otherwise" is the complement of "two or fewer": three or more oil counters grants hexproof.
 */
val EvolvedSpinoderm = card("Evolved Spinoderm") {
    manaCost = "{2}{G}{G}"
    typeLine = "Creature — Phyrexian Beast"
    power = 5
    toughness = 5
    oracleText = "This creature enters with four oil counters on it.\n" +
        "This creature has trample as long as it has two or fewer oil counters on it. Otherwise, it has hexproof.\n" +
        "At the beginning of your upkeep, remove an oil counter from this creature. Then if it has no " +
        "oil counters on it, sacrifice it."

    replacementEffect(EntersWithCounters(counterType = CounterType.OIL, count = 4, selfOnly = true))

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.TRAMPLE, Filters.Self),
            condition = Conditions.SourceCounterCountAtMost(CounterType.OIL, 2)
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.HEXPROOF, Filters.Self),
            condition = Conditions.SourceCounterCountAtLeast(CounterType.OIL, 3)
        )
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.RemoveCounters(CounterType.OIL, 1, EffectTarget.Self) then
            Effects.If(
                condition = Conditions.SourceCounterCountAtMost(CounterType.OIL, 0),
                then = Effects.SacrificeTarget(EffectTarget.Self),
            )
        description = "At the beginning of your upkeep, remove an oil counter from this creature. " +
            "Then if it has no oil counters on it, sacrifice it."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "166"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd4bcb42-f5cf-410d-8f20-1006b0f92abe.jpg?1783918016"
    }
}
