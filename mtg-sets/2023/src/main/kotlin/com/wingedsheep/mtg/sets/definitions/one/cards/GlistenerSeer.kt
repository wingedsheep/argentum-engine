package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters

/**
 * Glistener Seer
 * {U}
 * Creature — Phyrexian Advisor
 * 0/3
 *
 * This creature enters with three oil counters on it.
 * {T}, Remove an oil counter from this creature: Scry 1.
 */
val GlistenerSeer = card("Glistener Seer") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Phyrexian Advisor"
    power = 0
    toughness = 3
    oracleText = "This creature enters with three oil counters on it.\n" +
        "{T}, Remove an oil counter from this creature: Scry 1. " +
        "(Look at the top card of your library. You may put that card on the bottom.)"

    replacementEffect(EntersWithCounters(counterType = CounterType.OIL, count = 3, selfOnly = true))

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.RemoveCounterFromSelf(CounterType.OIL, 1))
        effect = Effects.Scry(1)
        description = "{T}, Remove an oil counter from this creature: Scry 1."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "54"
        artist = "Alix Branwyn"
        flavorText = "\"The oil is all-knowing. The trick is to ask the right questions.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c22aaaec-bad5-43e9-8e92-9c4bde95fcfd.jpg?1783918064"
    }
}
