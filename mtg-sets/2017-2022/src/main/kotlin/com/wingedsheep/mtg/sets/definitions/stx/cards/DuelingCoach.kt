package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dueling Coach — Strixhaven: School of Mages #15 (canonical printing)
 * {3}{W} · Creature — Human Monk · 2/2
 *
 * When this creature enters, put a +1/+1 counter on target creature.
 * {4}{W}, {T}: Put a +1/+1 counter on each creature you control with a +1/+1 counter on it.
 *
 * The activated ability is the Edgar, Moonlit Sovereign shape: a [Effects.ForEachInGroup] over
 * creatures you control that have a +1/+1 counter, adding one to each.
 */
val DuelingCoach = card("Dueling Coach") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Monk"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, put a +1/+1 counter on target creature.\n" +
        "{4}{W}, {T}: Put a +1/+1 counter on each creature you control with a +1/+1 counter on it."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val recipient = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, recipient)
        description = "When this creature enters, put a +1/+1 counter on target creature."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}{W}"), Costs.Tap)
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.withCounter(CounterType.PLUS_ONE_PLUS_ONE).youControl()),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity),
        )
        description = "{4}{W}, {T}: Put a +1/+1 counter on each creature you control with a +1/+1 " +
            "counter on it."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "15"
        artist = "Caio Monteiro"
        imageUri = "https://cards.scryfall.io/normal/front/4/5/45b3dbb0-0d68-4351-bfc9-a09c50454bf7.jpg?1783927391"
    }
}
