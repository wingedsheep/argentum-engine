package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Maulfist Doorbuster — Kaladesh #123
 * {3}{R} · Creature — Human Warrior · 4/2
 *
 * When this creature enters, you get {E}{E} (two energy counters).
 * Whenever this creature attacks, you may pay {E}. If you do, target creature can't block this turn.
 *
 * The target is chosen when the attack trigger goes on the stack; the energy is offered on
 * resolution ([Effects.MayPay], Eddytrail Hawk shape). The target can't block any creature, not
 * just the Doorbuster (2016-09-20 ruling), so [Effects.CantBlock] takes no attacker.
 */
val MaulfistDoorbuster = card("Maulfist Doorbuster") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Warrior"
    power = 4
    toughness = 2
    oracleText = "When this creature enters, you get {E}{E} (two energy counters).\n" +
        "Whenever this creature attacks, you may pay {E}. If you do, target creature can't block this turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(2)
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val t = target(TargetFilter.Creature)
        effect = Effects.MayPay(
            cost = Effects.PayExactCounters(CounterType.ENERGY, 1),
            then = Effects.CantBlock(t),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "123"
        artist = "Josh Hass"
        imageUri = "https://cards.scryfall.io/normal/front/5/3/53681384-b72d-42a6-b134-aa420115ea12.jpg?1783937191"
        ruling(
            "2016-09-20",
            "The target creature can't block any creatures this turn. The effect isn't restricted to just " +
                "whether the creature can block Maulfist Doorbuster."
        )
        ruling(
            "2024-06-07",
            "Some triggered abilities that state that you \"may pay\" a certain amount of {E} describe an effect " +
                "that happens \"If you do.\" In that case, no player may take actions to try to stop the ability's " +
                "effect after you make your choice."
        )
    }
}
