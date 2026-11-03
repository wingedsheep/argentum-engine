package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedByMoreThan
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hungering Hydra — Core Set 2019 #189
 * {X}{G} · Creature — Hydra · 0/0
 *
 * This creature enters with X +1/+1 counters on it.
 * This creature can't be blocked by more than one creature.
 * Whenever this creature is dealt damage, put that many +1/+1 counters on it.
 *
 * Krakilin's X-counters replacement, Professional Wrestler's one-blocker cap, and Broodhatch
 * Nantuko's self-damage trigger reading "that many" off the triggering damage. Lethal damage kills
 * the Hydra by state-based action before the trigger resolves, so it must survive to grow.
 */
val HungeringHydra = card("Hungering Hydra") {
    manaCost = "{X}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Hydra"
    power = 0
    toughness = 0
    oracleText = "This creature enters with X +1/+1 counters on it.\n" +
        "This creature can't be blocked by more than one creature.\n" +
        "Whenever this creature is dealt damage, put that many +1/+1 counters on it. " +
        "(It must survive the damage to get the counters.)"

    replacementEffect(EntersWithDynamicCounters(count = DynamicAmounts.xValue()))

    staticAbility {
        ability = CantBeBlockedByMoreThan(maxBlockers = 1)
    }

    triggeredAbility {
        trigger = Triggers.self.isDealtDamage()
        effect = Effects.AddDynamicCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            amount = DynamicAmounts.triggerDamageAmount(),
            target = EffectTarget.Self,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "189"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/8/4/84127b83-e75a-4f12-92ca-46f50bb89699.jpg?1783934533"
    }
}
