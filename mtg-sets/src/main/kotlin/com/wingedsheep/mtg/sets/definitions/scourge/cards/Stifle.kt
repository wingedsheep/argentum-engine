package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Stifle
 * {U}
 * Instant
 * Counter target activated or triggered ability.
 * (Mana abilities can't be targeted.)
 */
val Stifle = card("Stifle") {
    manaCost = "{U}"
    typeLine = "Instant"
    oracleText = "Counter target activated or triggered ability. (Mana abilities can't be targeted.)"

    spell {
        target = Targets.ActivatedOrTriggeredAbility
        effect = Effects.CounterAbility()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "52"
        artist = "Dany Orizio"
        flavorText = "\"If I wanted your opinion, I'd have told you what it was.\" —Pemmin, Riptide survivor"
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2d7643c0-b2db-478f-944e-b27b77bad3eb.jpg?1562527068"
        ruling("10/4/2004", "An activated ability has a 'Cost: Effect' format. A triggered ability starts with 'when', 'whenever', or 'at'.")
        ruling("10/4/2004", "Turn-based actions and special actions like the normal card draw, combat damage, or turning a face-down creature face up can't be targeted.")
        ruling("10/4/2004", "It can target delayed triggered abilities.")
    }
}
