package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Breeding Pool reprint in Edge of Eternities. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Dissension (`dis`) `cards/` package
 * (the card's earliest real printing); this file contributes only per-printing presentation data.
 */
val BreedingPoolReprint = Printing(
    oracleId = "20283c4a-f1f0-42f0-bc08-6da87474426b",
    name = "Breeding Pool",
    setCode = "EOE",
    collectorNumber = "251",
    scryfallId = "3c750d5a-f743-41ff-b5ba-02025ca0bec2",
    artist = "Constantin Marin",
    imageUri = "https://cards.scryfall.io/normal/front/3/c/3c750d5a-f743-41ff-b5ba-02025ca0bec2.jpg?1752947580",
    releaseDate = "2025-08-01",
    rarity = Rarity.RARE,
)
