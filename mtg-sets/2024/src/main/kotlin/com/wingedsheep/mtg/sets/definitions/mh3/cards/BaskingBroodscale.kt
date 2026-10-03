package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Basking Broodscale
 * {1}{G}
 * Creature — Eldrazi Lizard
 * 2/2
 *
 * Devoid
 * {1}{G}: Adapt 1.
 * Whenever one or more +1/+1 counters are put on this creature, you may create a 0/1 colorless
 * Eldrazi Spawn creature token with "Sacrifice this token: Add {C}."
 *
 * Adapt is the house composition `If(no +1/+1 counters, AddCounters)` (see Hydra Trainer). The
 * trigger is the per-batch `Triggers.self.getsCounters(+1/+1)` watcher, so it fires once per
 * placement event regardless of how many counters land, and also when it enters with counters.
 */
val BaskingBroodscale = card("Basking Broodscale") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Eldrazi Lizard"
    power = 2
    toughness = 2
    oracleText = "Devoid (This card has no color.)\n" +
        "{1}{G}: Adapt 1. (If this creature has no +1/+1 counters on it, put a +1/+1 counter on it.)\n" +
        "Whenever one or more +1/+1 counters are put on this creature, you may create a 0/1 colorless " +
        "Eldrazi Spawn creature token with \"Sacrifice this token: Add {C}.\""

    keywords(Keyword.DEVOID)

    // {1}{G}: Adapt 1.
    activatedAbility {
        cost = Costs.Mana("{1}{G}")
        effect = Effects.If(
            Conditions.Not(Conditions.SourceHasCounter(CounterType.PLUS_ONE_PLUS_ONE)),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
        )
    }

    triggeredAbility {
        trigger = Triggers.self.getsCounters(CounterType.PLUS_ONE_PLUS_ONE)
        optional = true
        effect = Effects.CreateEldraziSpawn()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "145"
        artist = "Caio Monteiro"
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5feba5d6-99a6-4e9b-8a7d-90d955868fc3.jpg?1783911263"
    }
}
