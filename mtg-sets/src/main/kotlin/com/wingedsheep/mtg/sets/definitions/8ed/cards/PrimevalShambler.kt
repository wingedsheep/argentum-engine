package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Primeval Shambler reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MMQ's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val PrimevalShamblerReprint = Printing(
    oracleId = "7a2722c6-04a0-4790-be45-dafb7832bb41",
    name = "Primeval Shambler",
    setCode = "8ED",
    collectorNumber = "156",
    artist = "Chippy",
    imageUri = "https://cards.scryfall.io/normal/front/e/4/e45931d2-3fbf-4509-aa41-13d7839578df.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)
