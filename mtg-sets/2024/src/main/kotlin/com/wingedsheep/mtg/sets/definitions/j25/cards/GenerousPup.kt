package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Generous Pup — Foundations Jumpstart #4 (canonical printing)
 * {1}{W} · Creature — Dog · 2/2
 *
 * Vigilance
 * Whenever one or more +1/+1 counters are put on this creature, put a +1/+1 counter on each other
 * creature you control. This ability triggers only once each turn.
 *
 * `Triggers.self.getsCounters` fires once per counter-placement event however many counters it
 * carries (the ruling: each other creature still gets exactly one), and `oncePerTurn` is the
 * TriggerDetector's generic per-(source, ability) cap that resets each turn.
 */
val GenerousPup = card("Generous Pup") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dog"
    oracleText = "Vigilance\n" +
        "Whenever one or more +1/+1 counters are put on this creature, put a +1/+1 counter on each other " +
        "creature you control. This ability triggers only once each turn."
    power = 2
    toughness = 2

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.getsCounters(CounterType.PLUS_ONE_PLUS_ONE)
        oncePerTurn = true
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl(), excludeSelf = true),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
        )
        description = "Whenever one or more +1/+1 counters are put on this creature, put a +1/+1 counter " +
            "on each other creature you control. This ability triggers only once each turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "4"
        artist = "Julia Metzger"
        flavorText = "They communicate via the universal language of treats."
        imageUri = "https://cards.scryfall.io/normal/front/7/3/730f6a8c-7119-4226-bf37-442cf5ec90c4.jpg?1783908869"
        ruling(
            "2024-11-08",
            "Generous Pup's last ability puts only one +1/+1 counter on each other creature you control, " +
                "no matter how many +1/+1 counters were put on Generous Pup."
        )
    }
}
