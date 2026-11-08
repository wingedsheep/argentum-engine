package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Aether Theorist — Kaladesh #37
 * {1}{U} · Creature — Vedalken Rogue · 1/3
 *
 * When this creature enters, you get {E}{E}{E} (three energy counters).
 * {T}, Pay {E}: Scry 1.
 */
val AetherTheorist = card("Aether Theorist") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Vedalken Rogue"
    power = 1
    toughness = 3
    oracleText = "When this creature enters, you get {E}{E}{E} (three energy counters).\n" +
        "{T}, Pay {E}: Scry 1. (Look at the top card of your library. You may put that card on the bottom.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(3)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayPlayerCounters(CounterType.ENERGY, 1))
        effect = Effects.Scry(1)
        description = "{T}, Pay {E}: Scry 1."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "37"
        artist = "Lucas Graciano"
        imageUri = "https://cards.scryfall.io/normal/front/8/8/882b5580-ee23-4d65-a2b3-82475d6faf8e.jpg?1783937224"
    }
}
