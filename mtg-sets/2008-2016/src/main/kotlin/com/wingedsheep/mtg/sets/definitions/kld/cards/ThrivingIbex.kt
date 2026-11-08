package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thriving Ibex — Kaladesh #31
 * {3}{W} · Creature — Goat · 2/4
 *
 * When this creature enters, you get {E}{E} (two energy counters).
 * Whenever this creature attacks, you may pay {E}{E}. If you do, put a +1/+1 counter on it.
 *
 * The energy is offered on resolution ([Effects.MayPay], Eddytrail Hawk shape).
 */
val ThrivingIbex = card("Thriving Ibex") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Goat"
    power = 2
    toughness = 4
    oracleText = "When this creature enters, you get {E}{E} (two energy counters).\n" +
        "Whenever this creature attacks, you may pay {E}{E}. If you do, put a +1/+1 counter on it."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(2)
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.MayPay(
            cost = Effects.PayExactCounters(CounterType.ENERGY, 2),
            then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "31"
        artist = "Sidharth Chaturvedi"
        flavorText = "Textiles at the Inventors' Fair feature threads from every corner of Kaladesh."
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8edc7c57-9298-4fad-a0c2-4b75b944e2ce.jpg?1783937228"
        ruling(
            "2024-06-07",
            "Some triggered abilities that state that you \"may pay\" a certain amount of {E} describe an effect " +
                "that happens \"If you do.\" In that case, no player may take actions to try to stop the ability's " +
                "effect after you make your choice."
        )
    }
}
