package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Giant Octopus
 * {3}{U}
 * Creature - Octopus
 * 3/3
 * (Vanilla creature)
 */
val GiantOctopus = card("Giant Octopus") {
    manaCost = "{3}{U}"
    typeLine = "Creature — Octopus"
    power = 3
    toughness = 3

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "56"
        artist = "John Matson"
        imageUri = "https://cards.scryfall.io/normal/front/4/5/4528edca-cc36-4f63-9615-24ca315d672c.jpg"
    }
}
