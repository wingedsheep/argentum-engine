package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thriving Grubs — Kaladesh #138
 * {1}{R} · Creature — Gremlin · 2/1
 *
 * When this creature enters, you get {E}{E} (two energy counters).
 * Whenever this creature attacks, you may pay {E}{E}. If you do, put a +1/+1 counter on it.
 *
 * The energy is offered on resolution ([Effects.MayPay], Thriving Turtle shape).
 */
val ThrivingGrubs = card("Thriving Grubs") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Gremlin"
    power = 2
    toughness = 1
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
        collectorNumber = "138"
        artist = "Steve Prescott"
        flavorText = "Some passengers ride without tickets."
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bbc3184a-eeda-4f22-92de-257c20cff6e2.jpg?1783937186"
        ruling(
            "2024-06-07",
            "Some triggered abilities that state that you \"may pay\" a certain amount of {E} describe an effect " +
                "that happens \"If you do.\" In that case, no player may take actions to try to stop the ability's " +
                "effect after you make your choice."
        )
    }
}
