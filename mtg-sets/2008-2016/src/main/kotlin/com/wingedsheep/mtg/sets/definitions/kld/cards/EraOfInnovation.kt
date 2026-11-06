package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Era of Innovation — Kaladesh #45
 * {1}{U} · Enchantment
 *
 * Whenever an artifact or Artificer you control enters, you may pay {1}. If you do, you get {E}{E}.
 * Pay six {E}, Sacrifice this enchantment: Draw three cards.
 *
 * One trigger over the union filter, so an Artificer artifact creature triggers it once (ruling).
 * The {1} is offered on resolution ([Effects.MayPay], Spirit Bonds shape).
 */
val EraOfInnovation = card("Era of Innovation") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Whenever an artifact or Artificer you control enters, you may pay {1}. If you do, you get {E}{E} (two energy counters).\n" +
        "Pay six {E}, Sacrifice this enchantment: Draw three cards."

    triggeredAbility {
        trigger = Triggers.a(
            (GameObjectFilter.Artifact or GameObjectFilter.Permanent.withSubtype(Subtype.ARTIFICER)).youControl()
        ).enters()
        effect = Effects.MayPay(
            cost = Effects.PayMana("{1}"),
            then = Effects.GetEnergy(2),
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.PayPlayerCounters(CounterType.ENERGY, 6), Costs.SacrificeSelf)
        effect = Effects.DrawCards(3)
        description = "Pay six {E}, Sacrifice this enchantment: Draw three cards."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "45"
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5dae3b1f-6a31-4a8a-9e44-063f8f670134.jpg?1783937223"
        ruling(
            "2016-09-20",
            "You can pay {1} only once each time Era of Innovation's first ability resolves. You can't pay more to get more than {E}{E}."
        )
        ruling(
            "2016-09-20",
            "If a creature that is both an artifact and an Artificer enters the battlefield under your control, " +
                "Era of Innovation's triggered ability triggers only once."
        )
    }
}
