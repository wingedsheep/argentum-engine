package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Vampire Interloper reprint in FDN.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ISD's `cards/` package (the card's earliest real printing). This file contributes only
 * the FDN-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val VampireInterloperReprint = Printing(
    oracleId = "b753d7dd-4c6b-4472-a009-671572f927eb",
    name = "Vampire Interloper",
    setCode = "FDN",
    collectorNumber = "756",
    artist = "James Ryman",
    imageUri = "https://cards.scryfall.io/normal/front/a/c/ac01d4eb-10ec-4bda-aa9b-04c74dabfcaf.jpg",
    releaseDate = "2024-11-15",
    rarity = Rarity.COMMON,
)
