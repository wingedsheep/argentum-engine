package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CounterKindSource
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Quarry Hauler — Amonkhet #181 (canonical printing)
 * {3}{G} · Creature — Camel · 4/3
 *
 * When this creature enters, for each kind of counter on target permanent, put another counter of
 * that kind on it or remove one from it.
 *
 * Same shape as Dramatist's Puppet: the kinds are read off the target as the trigger resolves; each
 * kind is a separate add-or-remove choice whose prompt names the kind ({counter}).
 */
val QuarryHauler = card("Quarry Hauler") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Camel"
    power = 4
    toughness = 3
    oracleText = "When this creature enters, for each kind of counter on target permanent, put another " +
        "counter of that kind on it or remove one from it."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val permanent = target(TargetFilter.Permanent)
        effect = Effects.ForEachCounterKindOn(
            permanent,
            Effects.ChooseAction(
                listOf(
                    EffectChoice(
                        "Put another {counter} counter on it",
                        Effects.AddCountersOfChosenKind(permanent, kindFrom = CounterKindSource.ITERATION)
                    ),
                    EffectChoice(
                        "Remove a {counter} counter from it",
                        Effects.RemoveCountersOfIteratedKind(permanent)
                    )
                )
            )
        )
        description = "When this creature enters, for each kind of counter on target permanent, put another " +
            "counter of that kind on it or remove one from it."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "181"
        artist = "David Gaillet"
        flavorText = "They labor beside the anointed, constructing the vast monuments of Naktamun."
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b2877c5-7680-4c2d-8e46-1d4548f0b03a.jpg?1783936470"
    }
}
