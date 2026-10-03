package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Walking Ballista
 * {X}{X}
 * Artifact Creature — Construct
 * 0/0
 * This creature enters with X +1/+1 counters on it.
 * {4}: Put a +1/+1 counter on this creature.
 * Remove a +1/+1 counter from this creature: It deals 1 damage to any target.
 *
 * Rock Hydra's X-counter entry ([EntersWithDynamicCounters] over the cast's X), plus
 * Monoskelion's remove-a-counter ping without the mana pip. With {X}{X}, X is half the mana spent
 * on the cost, so casting it for X = 2 costs four.
 */
val WalkingBallista = card("Walking Ballista") {
    manaCost = "{X}{X}"
    typeLine = "Artifact Creature — Construct"
    power = 0
    toughness = 0
    oracleText = "This creature enters with X +1/+1 counters on it.\n" +
        "{4}: Put a +1/+1 counter on this creature.\n" +
        "Remove a +1/+1 counter from this creature: It deals 1 damage to any target."

    replacementEffect(EntersWithDynamicCounters(count = DynamicAmounts.xValue()))

    activatedAbility {
        cost = Costs.Mana("{4}")
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.RemoveCounterFromSelf(CounterType.PLUS_ONE_PLUS_ONE)
        val t = target(Targets.Any)
        effect = Effects.DealDamage(1, t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "181"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/3/2/329a8738-3e17-403a-857a-0ba529ce8cd1.jpg?1783936718"
    }
}
