package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thriving Rhino — Kaladesh #171
 * {2}{G} · Creature — Rhino · 2/3
 *
 * When this creature enters, you get {E}{E} (two energy counters).
 * Whenever this creature attacks, you may pay {E}{E}. If you do, put a +1/+1 counter on it.
 *
 * The energy is offered on resolution ([Effects.MayPay], Thriving Turtle shape).
 */
val ThrivingRhino = card("Thriving Rhino") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Rhino"
    power = 2
    toughness = 3
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
        collectorNumber = "171"
        artist = "Matt Stewart"
        flavorText = "As unpredictable as aether, and just as hard to contain."
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5acf10ec-f2dd-4569-909b-ac52a9ed6adf.jpg?1783937171"
        ruling(
            "2024-06-07",
            "Some triggered abilities that state that you \"may pay\" a certain amount of {E} describe an effect " +
                "that happens \"If you do.\" In that case, no player may take actions to try to stop the ability's " +
                "effect after you make your choice."
        )
    }
}
