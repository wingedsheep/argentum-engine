package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Springleaf Drum reprint in ECL.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LRW's `cards/` package (the card's earliest real printing). This file contributes
 * only the ECL-specific presentation row — set, collector number, art — picked up
 * automatically by `CardDiscovery.findPrintingsIn` and surfaced via the set's
 * `printings`.
 */
val SpringleafDrumReprint = Printing(
    oracleId = "bdeb440d-a714-40e8-9038-f651e6ae45fb",
    name = "Springleaf Drum",
    setCode = "ECL",
    collectorNumber = "260",
    scryfallId = "e15ab0aa-4059-4923-9816-6f7a9e5b5a18",
    artist = "Cory Godbey",
    imageUri = "https://cards.scryfall.io/normal/front/e/1/e15ab0aa-4059-4923-9816-6f7a9e5b5a18.jpg?1767732925",
    releaseDate = "2026-01-23",
    rarity = Rarity.UNCOMMON,
)
