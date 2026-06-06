package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Coral Eel reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * POR's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val CoralEelReprint = Printing(
    oracleId = "10706fd1-7847-4316-be8d-59b56143ce45",
    name = "Coral Eel",
    setCode = "8ED",
    collectorNumber = "70",
    artist = "Una Fricker",
    imageUri = "https://cards.scryfall.io/normal/front/2/1/215244ee-0b78-4fd1-858f-d093a5d42939.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
