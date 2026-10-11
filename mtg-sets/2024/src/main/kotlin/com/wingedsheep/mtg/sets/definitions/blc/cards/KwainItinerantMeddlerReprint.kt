package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Kwain, Itinerant Meddler reprints in Bloomburrow Commander — the borderless printing (#90) and
 * the deck printing (#254). The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in the
 * Commander Legends (`cmr`) `cards/` package; these contribute only per-printing presentation data.
 */
val KwainItinerantMeddlerBorderlessReprint = Printing(
    oracleId = "ce6a85b5-e249-4582-a6be-ce200da5fd53",
    name = "Kwain, Itinerant Meddler",
    setCode = "BLC",
    collectorNumber = "90",
    scryfallId = "941f5231-5620-45e5-9f00-a7f64f0385dc",
    artist = "Mike Burns",
    imageUri = "https://cards.scryfall.io/normal/front/9/4/941f5231-5620-45e5-9f00-a7f64f0385dc.jpg?1783910708",
    releaseDate = "2024-08-02",
    rarity = Rarity.RARE,
)

val KwainItinerantMeddlerReprint = Printing(
    oracleId = "ce6a85b5-e249-4582-a6be-ce200da5fd53",
    name = "Kwain, Itinerant Meddler",
    setCode = "BLC",
    collectorNumber = "254",
    scryfallId = "cce29404-abfd-4c2c-90e5-5e04c111aa2b",
    artist = "Lucas Graciano",
    imageUri = "https://cards.scryfall.io/normal/front/c/c/cce29404-abfd-4c2c-90e5-5e04c111aa2b.jpg?1783910656",
    releaseDate = "2024-08-02",
    rarity = Rarity.RARE,
)
