package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Tempest Harvester
 * {1}{U}
 * Creature — Merfolk Wizard
 * 2/1
 *
 * When this creature enters, you get {E}{E} (two energy counters).
 * {T}, Pay {E}: Draw a card, then discard a card.
 */
val TempestHarvester = card("Tempest Harvester") {
    manaCost = "{1}{U}"
    typeLine = "Creature — Merfolk Wizard"
    power = 2
    toughness = 1
    oracleText = "When this creature enters, you get {E}{E} (two energy counters).\n" +
        "{T}, Pay {E}: Draw a card, then discard a card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(2)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayPlayerCounters(CounterType.ENERGY, 1))
        effect = Patterns.Hand.loot()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "73"
        artist = "Runa I. Rosenberger"
        flavorText = "\"The Golden City was a trinket compared to the power that lies beneath the waves.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/2/62a3fc3f-f5cd-45f1-a3d5-d90c9dd4f6c7.jpg?1783911285"
    }
}
