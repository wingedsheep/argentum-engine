package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kalonian Hydra
 * {3}{G}{G}
 * Creature — Hydra
 * 0/0
 * Trample
 * This creature enters with four +1/+1 counters on it.
 * Whenever this creature attacks, double the number of +1/+1 counters on each creature you control.
 *
 * The attack trigger visits every creature you control at resolution with
 * [Effects.ForEachInGroup] and doubles each one's +1/+1 counters via [Effects.DoubleCounters]
 * (put as many more as it already has, so a creature with none stays at none).
 */
val KalonianHydra = card("Kalonian Hydra") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Hydra"
    power = 0
    toughness = 0
    oracleText = "Trample\n" +
        "This creature enters with four +1/+1 counters on it.\n" +
        "Whenever this creature attacks, double the number of +1/+1 counters on each creature you control."

    keywords(Keyword.TRAMPLE)

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            count = 4,
            selfOnly = true
        )
    )

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.AllCreaturesYouControl,
            effect = Effects.DoubleCounters(CounterType.PLUS_ONE_PLUS_ONE, EffectTarget.IterationEntity)
        )
        description = "Whenever this creature attacks, double the number of +1/+1 counters on each creature you control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "181"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/4/3/438bd3c1-98f2-4fcc-8521-995c6c5c1a79.jpg?1783939903"
        ruling(
            "2013-07-01",
            "To double the number of +1/+1 counters on a creature, determine how many +1/+1 counters " +
                "are on the creature and put that many more on it. Effects that interact with counters " +
                "(such as the one created by Corpsejack Menace's ability) may change the number of " +
                "counters ultimately put on the creature.",
        )
    }
}
