package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thriving Skyclaw
 * {2}{R}{R}
 * Creature — Cat Dragon
 * 3/2
 *
 * Flying
 * When this creature enters, you get {E}{E}{E} (three energy counters).
 * Whenever this creature attacks, you may pay {E}{E}{E}. If you do, put a +1/+1 counter on it.
 *
 * The attack trigger's "may pay" is an [Effects.MayPay] over an exact energy payment — the prompt is
 * only offered when three energy can actually be paid.
 */
val ThrivingSkyclaw = card("Thriving Skyclaw") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Cat Dragon"
    power = 3
    toughness = 2
    oracleText = "Flying\n" +
        "When this creature enters, you get {E}{E}{E} (three energy counters).\n" +
        "Whenever this creature attacks, you may pay {E}{E}{E}. If you do, put a +1/+1 counter on it."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(3)
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.MayPay(
            cost = Effects.PayExactCounters(CounterType.ENERGY, 3),
            then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "141"
        artist = "Lucas Graciano"
        flavorText = "As wild as unbound aether, and just as dangerous."
        imageUri = "https://cards.scryfall.io/normal/front/4/6/465857f2-df08-4f7b-b4fe-b16831ac0eb1.jpg?1783911265"
    }
}
