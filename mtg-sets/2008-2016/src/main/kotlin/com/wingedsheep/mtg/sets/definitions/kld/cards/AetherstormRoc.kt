package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Aetherstorm Roc — Kaladesh #3
 * {2}{W}{W} · Creature — Bird · 3/3
 *
 * Flying
 * Whenever this creature or another creature you control enters, you get {E} (an energy counter).
 * Whenever this creature attacks, you may pay {E}{E}. If you do, put a +1/+1 counter on it and tap
 * up to one target creature defending player controls.
 *
 * "This creature or another" is the any-binding creature-you-control enters trigger (Kapsho
 * Kitefins shape). The attack trigger's optional target is chosen as it goes on the stack (Decimator
 * Beetle shape); the energy is offered on resolution ([Effects.MayPay], Thriving Turtle shape).
 */
val AetherstormRoc = card("Aetherstorm Roc") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "Whenever this creature or another creature you control enters, you get {E} (an energy counter).\n" +
        "Whenever this creature attacks, you may pay {E}{E}. If you do, put a +1/+1 counter on it and tap " +
        "up to one target creature defending player controls."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).enters()
        effect = Effects.GetEnergy(1)
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val t = target(TargetFilter.Creature.defendingPlayerControls(), optional = true)
        effect = Effects.MayPay(
            cost = Effects.PayExactCounters(CounterType.ENERGY, 2),
            then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self) then
                Effects.Tap(t),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "3"
        artist = "Scott Murphy"
        imageUri = "https://cards.scryfall.io/normal/front/7/9/79fb7541-7a27-40a5-aabd-fce26df03485.jpg?1783937237"
        ruling(
            "2016-09-20",
            "Aetherstorm Roc's second triggered ability resolves before blockers are chosen. A creature tapped " +
                "this way won't be able to block."
        )
        ruling(
            "2016-09-20",
            "You don't have to target a creature with Aetherstorm Roc's last ability. If you do choose a target, " +
                "and that target becomes illegal, the ability doesn't resolve and you won't be able to pay {E}{E} " +
                "to put a counter on Aetherstorm Roc."
        )
        ruling(
            "2016-09-20",
            "A creature entering the battlefield at the same time as Aetherstorm Roc causes its first triggered " +
                "ability to trigger."
        )
    }
}
