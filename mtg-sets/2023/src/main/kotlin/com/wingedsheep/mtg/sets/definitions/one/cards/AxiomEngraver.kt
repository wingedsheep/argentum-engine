package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters

/**
 * Axiom Engraver
 * {1}{R}
 * Creature — Phyrexian Wizard
 * 1/3
 *
 * This creature enters with two oil counters on it.
 * {T}, Remove an oil counter from this creature, Discard a card: Draw a card.
 */
val AxiomEngraver = card("Axiom Engraver") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Phyrexian Wizard"
    power = 1
    toughness = 3
    oracleText = "This creature enters with two oil counters on it.\n" +
        "{T}, Remove an oil counter from this creature, Discard a card: Draw a card."

    replacementEffect(EntersWithCounters(counterType = CounterType.OIL, count = 2, selfOnly = true))

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.RemoveCounterFromSelf(CounterType.OIL, 1), Costs.DiscardCard)
        effect = Effects.DrawCards(1)
        description = "{T}, Remove an oil counter from this creature, Discard a card: Draw a card."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "120"
        artist = "Pavel Kolomeyets"
        flavorText = "\"I will not rest until the truth of Phyrexia is inscribed across the Multiverse.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c9b3b785-396e-4240-bfa3-58ab53497686.jpg?1783918036"
    }
}
