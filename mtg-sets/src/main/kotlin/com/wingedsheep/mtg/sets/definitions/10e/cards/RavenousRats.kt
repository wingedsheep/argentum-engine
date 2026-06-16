package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Ravenous Rats reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * P02's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val RavenousRatsReprint = Printing(
    oracleId = "2fa1bbfd-92b5-482c-b32d-4cdc286474c4",
    name = "Ravenous Rats",
    setCode = "10E",
    collectorNumber = "171",
    artist = "Carl Critchlow",
    imageUri = "https://cards.scryfall.io/normal/front/0/3/03a8b556-6e18-46ee-80f8-cbd922e37432.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
