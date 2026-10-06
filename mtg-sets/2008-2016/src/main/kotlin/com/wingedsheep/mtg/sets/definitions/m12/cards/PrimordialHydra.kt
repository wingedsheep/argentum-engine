package com.wingedsheep.mtg.sets.definitions.m12.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Primordial Hydra
 * {X}{G}{G}
 * Creature — Hydra
 * 0/0
 *
 * This creature enters with X +1/+1 counters on it.
 * At the beginning of your upkeep, double the number of +1/+1 counters on this creature.
 * This creature has trample as long as it has ten or more +1/+1 counters on it.
 *
 * Doubling is "put as many +1/+1 counters on it as it already has" (Hydra's Growth shape). The
 * trample grant is gated on the +1/+1 counter count, not on power, per the 2011-09-22 ruling.
 */
val PrimordialHydra = card("Primordial Hydra") {
    manaCost = "{X}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Hydra"
    power = 0
    toughness = 0
    oracleText = "This creature enters with X +1/+1 counters on it.\n" +
        "At the beginning of your upkeep, double the number of +1/+1 counters on this creature.\n" +
        "This creature has trample as long as it has ten or more +1/+1 counters on it."

    replacementEffect(EntersWithDynamicCounters(count = DynamicAmounts.xValue()))

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.AddDynamicCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            amount = DynamicAmounts.countersOnSelf(CounterType.PLUS_ONE_PLUS_ONE),
            target = EffectTarget.Self
        )
    }

    staticAbility {
        condition = Conditions.SourceCounterCountAtLeast(CounterType.PLUS_ONE_PLUS_ONE, 10)
        ability = GrantKeyword(Keyword.TRAMPLE, Filters.Self)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "189"
        artist = "Aleksi Briclot"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3dcc5521-df8f-4992-b93e-e430d8cc7715.jpg?1783941056"
        ruling(
            "2011-09-22",
            "Consider only the number of +1/+1 counters on Primordial Hydra when determining if it has trample, " +
                "not its power and toughness. For example, a Primordial Hydra with six +1/+1 counters on it that's " +
                "been the target of Titanic Growth (giving it +4/+4) would not have trample."
        )
    }
}
