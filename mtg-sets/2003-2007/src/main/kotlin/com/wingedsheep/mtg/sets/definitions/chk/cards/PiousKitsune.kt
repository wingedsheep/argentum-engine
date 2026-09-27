package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pious Kitsune
 * {2}{W}
 * Creature — Fox Cleric
 * 1/2
 * At the beginning of your upkeep, put a devotion counter on this creature. Then if a creature
 * named Eight-and-a-Half-Tails is on the battlefield, you gain 1 life for each devotion counter on
 * this creature.
 * {T}, Remove a devotion counter from this creature: You gain 1 life.
 *
 * The "then if" is checked at resolution, after the counter lands, so the new counter is counted.
 * "On the battlefield" with no controller means any player's Eight-and-a-Half-Tails.
 */
val PiousKitsune = card("Pious Kitsune") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Fox Cleric"
    power = 1
    toughness = 2
    oracleText = "At the beginning of your upkeep, put a devotion counter on this creature. Then if a " +
        "creature named Eight-and-a-Half-Tails is on the battlefield, you gain 1 life for each devotion " +
        "counter on this creature.\n" +
        "{T}, Remove a devotion counter from this creature: You gain 1 life."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.AddCounters(CounterType.DEVOTION, 1, EffectTarget.Self) then
            Effects.If(
                Conditions.AnyPlayerControls(GameObjectFilter.Creature.named("Eight-and-a-Half-Tails")),
                Effects.GainLife(DynamicAmounts.countersOnSelf(CounterType.DEVOTION))
            )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.RemoveCounterFromSelf(CounterType.DEVOTION, 1))
        effect = Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "38"
        artist = "Anthony S. Waters"
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bb4124b9-5169-471e-b805-ceeffeec9184.jpg?1783944334"
    }
}
