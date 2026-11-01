package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Benthic Biomancer — Ravnica Allegiance #32
 * {U} · Creature — Merfolk Wizard Mutant · 1 / 1
 *
 * Incubation Druid's `Effects.Adapt` activated ability plus Herd Baloth's per-placement
 * `Triggers.self.getsCounters` watcher, resolving a `Patterns.Hand.loot`.
 */
val BenthicBiomancer = card("Benthic Biomancer") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Wizard Mutant"
    power = 1
    toughness = 1
    oracleText = "{1}{U}: Adapt 1. (If this creature has no +1/+1 counters on it, put a +1/+1 counter on it.)\n" +
        "Whenever one or more +1/+1 counters are put on this creature, draw a card, then discard a card."

    activatedAbility {
        cost = Costs.Mana("{1}{U}")
        effect = Effects.Adapt(1)
        description = "{1}{U}: Adapt 1."
    }

    triggeredAbility {
        trigger = Triggers.self.getsCounters(CounterType.PLUS_ONE_PLUS_ONE)
        effect = Patterns.Hand.loot(draw = 1, discard = 1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "32"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7b0cb4d6-350b-4e66-b035-dac7b3ba77cc.jpg?1783933714"
        ruling("2019-01-25", "An ability that triggers when counters are put on a permanent will trigger if that permanent somehow enters the battlefield with those counters.")
        ruling("2019-01-25", "You draw a card and discard a card all while Benthic Biomancer's triggered ability is resolving. Nothing can happen between the two, and no player may choose to take actions.")
        ruling("2019-01-25", "You can always activate an ability that will cause a creature to adapt. As that ability resolves, if the creature has a +1/+1 counter on it for any reason, you simply won't put any +1/+1 counters on it.")
        ruling("2019-01-25", "If a creature somehow loses all of its +1/+1 counters, it can adapt again and get more +1/+1 counters.")
    }
}
