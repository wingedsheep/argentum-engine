package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Diregraf Ghoul reprint in FDN.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ISD's `cards/` package (the card's earliest real printing). This file contributes only
 * the FDN-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val DiregrafGhoulReprint = Printing(
    oracleId = "6048fc70-0dcc-4b54-977d-16e240225f82",
    name = "Diregraf Ghoul",
    setCode = "FDN",
    collectorNumber = "171",
    artist = "Dave Kendall",
    imageUri = "https://cards.scryfall.io/normal/front/4/6/4682012c-d7e0-4257-b538-3de497507464.jpg",
    releaseDate = "2024-11-15",
    rarity = Rarity.UNCOMMON,
)
