package com.wingedsheep.mtg.sets.definitions.tle.cards

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
 * Bumi, Eclectic Earthbender
 * {3}{G}{G}
 * Legendary Creature — Human Noble Ally
 * 4/4
 * When Bumi enters, earthbend 1.
 * Whenever Bumi attacks, put two +1/+1 counters on each land creature you control.
 *
 * The ETB is the standard [Effects.Earthbend] over a targeted land you control. The attack trigger
 * is [Effects.ForEachInGroup] over land creatures you control (projected — earthbended lands and
 * manlands alike) putting two +1/+1 counters on each; no targets, so it always resolves.
 */
val BumiEclecticEarthbender = card("Bumi, Eclectic Earthbender") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Noble Ally"
    power = 4
    toughness = 4
    oracleText = "When Bumi enters, earthbend 1. (Target land you control becomes a 0/0 creature with " +
        "haste that's still a land. Put a +1/+1 counter on it. When it dies or is exiled, return it " +
        "to the battlefield tapped.)\n" +
        "Whenever Bumi attacks, put two +1/+1 counters on each land creature you control."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val land = target(TargetFilter.Land.youControl())
        effect = Effects.Earthbend(1, land)
        description = "When Bumi enters, earthbend 1."
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.ForEachInGroup(
            GroupFilter((GameObjectFilter.Creature and GameObjectFilter.Land).youControl()),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.IterationEntity)
        )
        description = "Whenever Bumi attacks, put two +1/+1 counters on each land creature you control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "248"
        artist = "Olena Richards"
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f324a384-7380-4f6e-bbba-fac1f2a01b5d.jpg?1783904775"
        ruling("2025-10-02", "You may target a land that is already a creature, perhaps because of a previous earthbend ability. The land will get the +1/+1 counters, gain haste, and have its base power and toughness set to 0/0.")
        ruling("2025-10-02", "If a land was animated by earthbend and would go to any zone other than the graveyard or exile, it will not be returned to the battlefield by the delayed triggered ability created by earthbend.")
    }
}
