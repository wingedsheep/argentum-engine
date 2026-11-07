package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Voltaic Brawler — Kaladesh #189
 * {R}{G} · Creature — Human Warrior · 3/2
 *
 * When this creature enters, you get {E}{E} (two energy counters).
 * Whenever this creature attacks, you may pay {E}. If you do, it gets +1/+1 and gains trample until
 * end of turn.
 *
 * The energy is offered on resolution ([Effects.MayPay], Riparian Tiger shape); the pump and the
 * trample grant chain on the paid branch (Dire Fleet Warmonger shape).
 */
val VoltaicBrawler = card("Voltaic Brawler") {
    manaCost = "{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Human Warrior"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, you get {E}{E} (two energy counters).\n" +
        "Whenever this creature attacks, you may pay {E}. If you do, it gets +1/+1 and gains trample " +
        "until end of turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(2)
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.MayPay(
            cost = Effects.PayExactCounters(CounterType.ENERGY, 1),
            then = Effects.ModifyStats(1, 1, EffectTarget.Self) then
                Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.Self),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "189"
        artist = "Raymond Swanland"
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cb038fd3-51b7-4de7-9b38-cbad7c8717c2.jpg?1783937165"
        ruling(
            "2024-06-07",
            "Some triggered abilities state that you \"may pay\" a certain amount of {E}. You can't pay that " +
                "amount multiple times to multiply the effect. You simply choose whether or not to pay that " +
                "amount of {E} as the ability resolves."
        )
        ruling(
            "2024-06-07",
            "Some triggered abilities that state that you \"may pay\" a certain amount of {E} describe an effect " +
                "that happens \"If you do.\" In that case, no player may take actions to try to stop the ability's " +
                "effect after you make your choice."
        )
    }
}
