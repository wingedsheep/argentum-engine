package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Master Decoy reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * TMP's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val MasterDecoyReprint = Printing(
    oracleId = "94a16609-8529-4301-8a3d-47ced194f7cd",
    name = "Master Decoy",
    setCode = "8ED",
    collectorNumber = "29",
    artist = "Ben Thompson",
    imageUri = "https://cards.scryfall.io/normal/front/2/f/2fc58f97-39ee-4d89-884f-88f9edc3459f.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
