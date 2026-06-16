package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Deathmark reprint in FDN.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * CSP's `cards/` package (the card's earliest real printing). This file contributes only
 * the FDN-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val DeathmarkReprint = Printing(
    oracleId = "cf09b0af-3cf1-4486-8f65-8cfd2410314a",
    name = "Deathmark",
    setCode = "FDN",
    collectorNumber = "601",
    artist = "Jeremy Jarvis",
    imageUri = "https://cards.scryfall.io/normal/front/4/2/42592d75-593e-4719-b13b-bca374017e79.jpg",
    releaseDate = "2024-11-15",
    rarity = Rarity.UNCOMMON,
)
