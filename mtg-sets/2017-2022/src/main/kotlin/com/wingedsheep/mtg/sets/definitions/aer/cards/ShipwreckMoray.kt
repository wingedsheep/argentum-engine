package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Shipwreck Moray
 * {3}{U}
 * Creature — Fish
 * 0/5
 * When this creature enters, you get {E}{E}{E}{E} (four energy counters).
 * Pay {E}: This creature gets +2/-2 until end of turn.
 */
val ShipwreckMoray = card("Shipwreck Moray") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Fish"
    power = 0
    toughness = 5
    oracleText = "When this creature enters, you get {E}{E}{E}{E} (four energy counters).\n" +
        "Pay {E}: This creature gets +2/-2 until end of turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(4)
    }

    activatedAbility {
        cost = Costs.PayPlayerCounters(CounterType.ENERGY, 1)
        effect = Effects.ModifyStats(2, -2, EffectTarget.Self)
        description = "Pay {E}: This creature gets +2/-2 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "45"
        artist = "Mike Bierek"
        flavorText = "Shipwrecks do wonders for the local fauna, providing both shelter and aether."
        imageUri = "https://cards.scryfall.io/normal/front/2/8/284c6de3-4e09-40d9-afdb-89ff08e1844b.jpg?1783936770"
        ruling(
            "2024-06-07",
            "If an effect says you get one or more {E}, you get that many energy counters. To pay one or more " +
                "{E}, you lose that many energy counters. You can't pay more energy counters than you have. Any " +
                "effects that interact with counters a player gets, has, or loses can interact with energy counters."
        )
    }
}
