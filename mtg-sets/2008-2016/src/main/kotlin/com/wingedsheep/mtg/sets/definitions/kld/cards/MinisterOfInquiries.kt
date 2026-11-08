package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Minister of Inquiries — Kaladesh #57
 * {U} · Creature — Vedalken Advisor · 1/2
 *
 * When this creature enters, you get {E}{E} (two energy counters).
 * {T}, Pay {E}: Target player mills three cards.
 */
val MinisterOfInquiries = card("Minister of Inquiries") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Vedalken Advisor"
    power = 1
    toughness = 2
    oracleText = "When this creature enters, you get {E}{E} (two energy counters).\n" +
        "{T}, Pay {E}: Target player mills three cards."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(2)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayPlayerCounters(CounterType.ENERGY, 1))
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(3, player)
        description = "{T}, Pay {E}: Target player mills three cards."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "57"
        artist = "Dan Murayama Scott"
        flavorText = "\"This research violates Consulate law.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0e6c31d1-6e2a-41b3-aa4f-d30a7cd997d2.jpg?1783937216"
    }
}
