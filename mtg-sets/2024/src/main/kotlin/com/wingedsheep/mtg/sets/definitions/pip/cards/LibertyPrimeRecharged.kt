package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect

/**
 * Liberty Prime, Recharged
 * {2}{U}{R}{W}
 * Legendary Artifact Creature — Robot
 * 8/8
 *
 * Vigilance, trample, haste
 * Whenever Liberty Prime attacks or blocks, sacrifice it unless you pay {E}{E} (two energy counters).
 * {2}, {T}, Sacrifice an artifact: You get {E}{E} and draw a card.
 *
 * "Attacks or blocks" is two triggered abilities sharing one effect (the Takenuma Bleeder shape).
 * The upkeep-style tax is a resolution-time [Effects.PayOrSuffer] over exactly two energy (the
 * Electrozoa shape); with fewer than two energy there's nothing to pay and Liberty Prime is
 * sacrificed. The sacrificed artifact may be Liberty Prime itself.
 */
val LibertyPrimeRecharged = card("Liberty Prime, Recharged") {
    manaCost = "{2}{U}{R}{W}"
    colorIdentity = "WUR"
    typeLine = "Legendary Artifact Creature — Robot"
    power = 8
    toughness = 8
    oracleText = "Vigilance, trample, haste\n" +
        "Whenever Liberty Prime attacks or blocks, sacrifice it unless you pay {E}{E} (two energy counters).\n" +
        "{2}, {T}, Sacrifice an artifact: You get {E}{E} and draw a card."

    keywords(Keyword.VIGILANCE, Keyword.TRAMPLE, Keyword.HASTE)

    val energyTax = Effects.PayOrSuffer(
        cost = Costs.pay.PayPlayerCounters(CounterType.ENERGY, 2),
        suffer = SacrificeSelfEffect,
    )

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = energyTax
    }

    triggeredAbility {
        trigger = Triggers.self.blocks()
        effect = energyTax
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}"),
            Costs.Tap,
            Costs.Sacrifice(GameObjectFilter.Artifact),
        )
        effect = Effects.GetEnergy(2) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "5"
        artist = "Pascal Quidault"
        flavorText = "\"Democracy is nonnegotiable.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2da11337-ceb2-4744-9696-c06fec2f2daa.jpg?1783912422"
    }
}
