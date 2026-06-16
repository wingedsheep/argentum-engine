package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Deathmark reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * CSP's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val DeathmarkReprint = Printing(
    oracleId = "cf09b0af-3cf1-4486-8f65-8cfd2410314a",
    name = "Deathmark",
    setCode = "10E",
    collectorNumber = "134",
    artist = "Jeremy Jarvis",
    imageUri = "https://cards.scryfall.io/normal/front/5/f/5f9271cc-6525-4a44-af24-915f0ba5afa9.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)
