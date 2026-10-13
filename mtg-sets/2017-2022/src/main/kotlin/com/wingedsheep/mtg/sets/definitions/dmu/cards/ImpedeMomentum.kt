package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Impede Momentum
 * {1}{U}
 * Sorcery
 * Tap target creature and put three stun counters on it.
 * Scry 1.
 */
val ImpedeMomentum = card("Impede Momentum") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Tap target creature and put three stun counters on it. (If a permanent with a stun counter would become untapped, remove one from it instead.)\n" +
        "Scry 1. (Look at the top card of your library. You may put that card on the bottom.)"

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.Tap(t) then
            Effects.AddCounters(counterType = CounterType.STUN, count = 3, target = t) then
            Patterns.Library.scry(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "54"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/2/5/259c04f7-86f2-4584-97fb-8278dca9a213.jpg?1783921349"
        ruling(
            "2022-09-09",
            "You may target a creature that is already tapped with Impede Momentum. If the target creature is tapped as it resolves, you will still put three stun counters on it."
        )
    }
}
