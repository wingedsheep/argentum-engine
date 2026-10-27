package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Oran-Rief Ooze — Zendikar Rising #198
 * {2}{G} · Creature — Ooze · 2/2
 *
 * When this creature enters, put a +1/+1 counter on target creature you control.
 * Whenever this creature attacks, put a +1/+1 counter on each attacking creature with a +1/+1
 * counter on it.
 *
 * The attack trigger checks counters on resolution, so the Ooze itself is included once it has
 * a +1/+1 counter (e.g. from its own enter trigger).
 */
val OranRiefOoze = card("Oran-Rief Ooze") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Ooze"
    oracleText = "When this creature enters, put a +1/+1 counter on target creature you control.\n" +
        "Whenever this creature attacks, put a +1/+1 counter on each attacking creature with a +1/+1 counter on it."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Creature.youControl())
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t)
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.attacking().withCounter(CounterType.PLUS_ONE_PLUS_ONE)),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "198"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd2ad967-dd77-4c3a-b53c-2929ecf7750f.jpg"
    }
}
