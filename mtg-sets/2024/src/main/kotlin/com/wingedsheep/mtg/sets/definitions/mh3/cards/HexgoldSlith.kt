package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hexgold Slith
 * {1}{W}
 * Creature — Slith
 * 2/1
 *
 * When this creature enters, you get {E}{E} (two energy counters).
 * Whenever this creature attacks, you may pay {E}{E}. If you do, it gains first strike until end of turn.
 * Whenever this creature deals combat damage to a player, put a +1/+1 counter on it.
 *
 * The attack trigger's "may pay" is an [Effects.MayPay] over an exact energy payment — the prompt is
 * only offered when two energy can actually be paid.
 */
val HexgoldSlith = card("Hexgold Slith") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Slith"
    power = 2
    toughness = 1
    oracleText = "When this creature enters, you get {E}{E} (two energy counters).\n" +
        "Whenever this creature attacks, you may pay {E}{E}. If you do, it gains first strike until end of turn.\n" +
        "Whenever this creature deals combat damage to a player, put a +1/+1 counter on it."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(2)
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.MayPay(
            cost = Effects.PayExactCounters(CounterType.ENERGY, 2),
            then = Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.Self),
        )
    }

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "30"
        artist = "Michele Giorgi"
        imageUri = "https://cards.scryfall.io/normal/front/2/0/20a85b6d-fb2b-4359-9dd6-081d8722e580.jpg?1783911301"
    }
}
