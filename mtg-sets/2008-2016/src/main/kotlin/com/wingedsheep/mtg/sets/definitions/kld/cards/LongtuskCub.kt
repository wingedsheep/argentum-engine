package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Longtusk Cub — Kaladesh #161
 * {1}{G} · Creature — Cat · 2/2
 *
 * Whenever this creature deals combat damage to a player, you get {E}{E} (two energy counters).
 * Pay {E}{E}: Put a +1/+1 counter on this creature.
 */
val LongtuskCub = card("Longtusk Cub") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Cat"
    power = 2
    toughness = 2
    oracleText = "Whenever this creature deals combat damage to a player, you get {E}{E} (two energy counters).\n" +
        "Pay {E}{E}: Put a +1/+1 counter on this creature."

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.GetEnergy(2)
    }

    activatedAbility {
        cost = Costs.PayPlayerCounters(CounterType.ENERGY, 2)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Pay {E}{E}: Put a +1/+1 counter on this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "161"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c4de778a-4419-49a5-9dcf-5d0095c873fa.jpg?1783937176"
    }
}
