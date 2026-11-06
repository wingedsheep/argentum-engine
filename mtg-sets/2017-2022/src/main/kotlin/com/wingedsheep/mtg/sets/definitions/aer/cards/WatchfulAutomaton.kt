package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Watchful Automaton
 * {3}
 * Artifact Creature — Construct
 * 2/2
 * {2}{U}: Scry 1.
 */
val WatchfulAutomaton = card("Watchful Automaton") {
    manaCost = "{3}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Construct"
    power = 2
    toughness = 2
    oracleText = "{2}{U}: Scry 1. (Look at the top card of your library. You may put that card on the bottom.)"

    activatedAbility {
        cost = Costs.Mana("{2}{U}")
        effect = Effects.Scry(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "182"
        artist = "Eric Deschamps"
        flavorText = "\"Before we can destroy our enemies, we must find them. We must peer into every wretched " +
            "hovel and every teeming market.\"\n—Baral, Chief of Compliance"
        imageUri = "https://cards.scryfall.io/normal/front/1/3/138b5fde-417d-4860-aca9-eae7f78b5768.jpg"
    }
}
