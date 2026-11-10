package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gladehart Cavalry — Oath of the Gatewatch #132 (canonical printing)
 * {5}{G}{G} · Creature — Elf Knight · 6/6
 *
 * When this creature enters, support 6. (Put a +1/+1 counter on each of up to six other target creatures.)
 * Whenever a creature you control with a +1/+1 counter on it dies, you gain 2 life.
 *
 * Support on a permanent targets *other* creatures (any controller), as on Expedition Raptor. The dies
 * trigger is the ANY binding with a `withCounter` filter (Laid to Rest's shape): the engine matches
 * zone-change trigger filters against the dying creature's last-known counters, and the Cavalry itself
 * counts if it somehow has a +1/+1 counter.
 */
val GladehartCavalry = card("Gladehart Cavalry") {
    manaCost = "{5}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Knight"
    power = 6
    toughness = 6
    oracleText = "When this creature enters, support 6. (Put a +1/+1 counter on each of up to six other " +
        "target creatures.)\nWhenever a creature you control with a +1/+1 counter on it dies, you gain 2 life."

    triggeredAbility {
        trigger = Triggers.self.enters()
        targets(TargetFilter.OtherCreature, count = 6, optional = true)
        effect = Effects.ForEachTarget(
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0)),
        )
        description = "When this creature enters, support 6 — put a +1/+1 counter on each of up " +
            "to six other target creatures."
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl().withCounter(CounterType.PLUS_ONE_PLUS_ONE)).dies()
        effect = Effects.GainLife(2)
        description = "Whenever a creature you control with a +1/+1 counter on it dies, you gain 2 life."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "132"
        artist = "Steven Belledin"
        imageUri = "https://cards.scryfall.io/normal/front/2/8/28ffd3f4-a034-4cb2-9270-bdb5197f1bef.jpg?1783937902"
        ruling("2016-01-22", "If Gladehart Cavalry dies at the same time as a creature you control with a +1/+1 counter on it, its ability will trigger.")
        ruling("2016-01-22", "If Gladehart Cavalry somehow gets a +1/+1 counter, it dying will cause its own ability to trigger.")
        ruling("2016-01-22", "You gain 2 life per creature, not per +1/+1 counter.")
        ruling("2016-01-22", "You can't put more than one +1/+1 counter on any one target using the support action.")
        ruling("2016-01-22", "Support can target a creature you don't control.")
    }
}
