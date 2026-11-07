package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Whirler Virtuoso — Kaladesh #190
 * {1}{U}{R} · Creature — Vedalken Artificer · 2/3
 *
 * When this creature enters, you get {E}{E}{E} (three energy counters).
 * Pay {E}{E}{E}: Create a 1/1 colorless Thopter artifact creature token with flying.
 */
val WhirlerVirtuoso = card("Whirler Virtuoso") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Vedalken Artificer"
    power = 2
    toughness = 3
    oracleText = "When this creature enters, you get {E}{E}{E} (three energy counters).\n" +
        "Pay {E}{E}{E}: Create a 1/1 colorless Thopter artifact creature token with flying."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(3)
    }

    activatedAbility {
        cost = Costs.PayPlayerCounters(CounterType.ENERGY, 3)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = emptySet(),
            creatureTypes = setOf("Thopter"),
            keywords = setOf(Keyword.FLYING),
            artifactToken = true,
        )
        description = "Pay {E}{E}{E}: Create a 1/1 colorless Thopter artifact creature token with flying."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "190"
        artist = "Lake Hurwitz"
        flavorText = "The first tentative flaps are like a song to his soul."
        imageUri = "https://cards.scryfall.io/normal/front/a/e/ae429806-28b5-4bee-b0e1-1bd876f282c2.jpg?1783937165"
    }
}
