package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Planar Portal
 * {6}
 * Artifact
 * {6}, {T}: Search your library for a card, put that card into your hand, then shuffle.
 */
val PlanarPortal = card("Planar Portal") {
    manaCost = "{6}"
    typeLine = "Artifact"
    oracleText = "{6}, {T}: Search your library for a card, put that card into your hand, then shuffle."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{6}"), Costs.Tap)
        effect = LibraryPatterns.searchLibrary()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "308"
        artist = "Mark Tedin"
        imageUri = "https://cards.scryfall.io/normal/front/2/4/24315eaa-ef55-4fd6-9145-e75b3de6f492.jpg?1562902264"
    }
}
