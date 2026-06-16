package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Fractal Mascot
 * {4}{G}{U}
 * Creature — Fractal Elk
 * 6/6
 *
 * Trample
 * When this creature enters, tap target creature an opponent controls. Put a stun
 * counter on it. (If a permanent with a stun counter would become untapped, remove
 * one from it instead.)
 *
 * The ETB binds a single target — a creature an opponent controls — then taps it and
 * places a stun counter on that same targeted creature. Composed from atomic Tap +
 * AddCounters facades; the stun-counter replacement effect (untap → remove a stun
 * counter instead) is handled engine-side wherever a permanent carries one.
 */
val FractalMascot = card("Fractal Mascot") {
    manaCost = "{4}{G}{U}"
    colorIdentity = "UG"
    typeLine = "Creature — Fractal Elk"
    power = 6
    toughness = 6
    oracleText = "Trample\n" +
        "When this creature enters, tap target creature an opponent controls. Put a stun " +
        "counter on it. (If a permanent with a stun counter would become untapped, remove " +
        "one from it instead.)"

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target(
            "target creature an opponent controls",
            TargetCreature(filter = TargetFilter.CreatureOpponentControls)
        )
        effect = Effects.Tap(creature)
            .then(Effects.AddCounters(Counters.STUN, 1, creature))
        description = "When this creature enters, tap target creature an opponent controls. " +
            "Put a stun counter on it."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "189"
        artist = "Manuel Castañón"
        flavorText = "The nature of theory given form and function."
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cf5b19e3-eed1-4b36-9756-660ffb3baa08.jpg?1775938311"
    }
}
