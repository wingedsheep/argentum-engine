package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Overgrowth Elemental
 * {2}{G}
 * Creature — Elemental
 * 3/2
 * When this creature enters, put a +1/+1 counter on another target Elemental you control.
 * Whenever another creature you control dies, you gain 1 life. If that creature was an Elemental,
 * put a +1/+1 counter on this creature.
 *
 * "If that creature was an Elemental" reads the dead creature's last-known subtypes
 * (`TriggeringEntityWas`), checked on resolution, not as an intervening-if — the life gain happens
 * either way.
 */
val OvergrowthElemental = card("Overgrowth Elemental") {
    manaCost = "{2}{G}"
    typeLine = "Creature — Elemental"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, put a +1/+1 counter on another target Elemental you control.\n" +
        "Whenever another creature you control dies, you gain 1 life. If that creature was an Elemental, " +
        "put a +1/+1 counter on this creature."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Permanent.withSubtype("Elemental").youControl().other())
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t)
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).dies()
        effect = Effects.GainLife(1) then
            Effects.If(
                Conditions.TriggeringEntityWas(GameObjectFilter.Creature.withSubtype("Elemental")),
                Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
            )
        description = "Whenever another creature you control dies, you gain 1 life. If that creature " +
            "was an Elemental, put a +1/+1 counter on this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "187"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c9a13eb-2daf-43e0-b175-c6ec5b9e191e.jpg?1783932960"
        ruling(
            "2019-07-12",
            "If Overgrowth Elemental dies at the same time as one or more other creatures you control, " +
                "Overgrowth Elemental's last ability triggers for each of those other creatures. You will " +
                "gain 1 life for each of them, but if any of those creatures were Elementals, Overgrowth " +
                "Elemental won't get a +1/+1 counter since it's already left the battlefield."
        )
    }
}
