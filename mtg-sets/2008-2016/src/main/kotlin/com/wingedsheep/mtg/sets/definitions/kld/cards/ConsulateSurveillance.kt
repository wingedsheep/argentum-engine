package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.PreventionSourceFilter

/**
 * Consulate Surveillance — Kaladesh #10
 * {3}{W} · Enchantment
 *
 * When this enchantment enters, you get {E}{E}{E}{E} (four energy counters).
 * Pay {E}{E}: Prevent all damage that would be dealt to you this turn by a source of your choice.
 *
 * The energy is an activation cost ([Costs.PayPlayerCounters], Plasma Caster shape); the source is
 * chosen on resolution, untargeted ([PreventionSourceFilter.Chosen], Samite Ministration shape).
 */
val ConsulateSurveillance = card("Consulate Surveillance") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, you get {E}{E}{E}{E} (four energy counters).\n" +
        "Pay {E}{E}: Prevent all damage that would be dealt to you this turn by a source of your choice."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(4)
    }

    activatedAbility {
        cost = Costs.PayPlayerCounters(CounterType.ENERGY, 2)
        effect = Effects.PreventDamage(sources = PreventionSourceFilter.Chosen())
        description = "Pay {E}{E}: Prevent all damage that would be dealt to you this turn by a source of your choice."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "10"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/7/2/72c92238-7765-48ee-a799-9e8b68ec118d.jpg?1783937234"
        ruling(
            "2016-09-20",
            "If the chosen source would deal damage to you and one or more other players or permanents, only the " +
                "damage that would be dealt to you is prevented."
        )
        ruling(
            "2016-09-20",
            "Consulate Surveillance's second ability doesn't target anything. You choose a source of damage as it resolves."
        )
    }
}
