package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Empyreal Voyager
 * {1}{G}{U}
 * Creature — Vedalken Scout
 * 2/3
 * Flying, trample
 * Whenever this creature deals combat damage to a player, you get that many {E} (energy counters).
 */
val EmpyrealVoyager = card("Empyreal Voyager") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Vedalken Scout"
    oracleText = "Flying, trample\n" +
        "Whenever this creature deals combat damage to a player, you get that many {E} (energy counters)."
    power = 2
    toughness = 3

    keywords(Keyword.FLYING, Keyword.TRAMPLE)

    // "That many" is the combat damage this creature just dealt to the player.
    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.AddDynamicCounters(
            CounterType.ENERGY,
            DynamicAmounts.triggerDamageAmount(),
            EffectTarget.Controller
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "180"
        artist = "David Gaillet"
        flavorText = "\"For years, I studied the aethersphere from afar, but it was not until I touched the aether for myself that I came to know the power contained in the swirling sky.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/5/4554b10e-9c3e-4eb4-b0fe-044e483e872f.jpg?1783937169"
    }
}
