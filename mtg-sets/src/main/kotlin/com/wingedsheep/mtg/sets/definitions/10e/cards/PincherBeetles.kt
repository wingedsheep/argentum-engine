package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Pincher Beetles reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * TMP's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val PincherBeetlesReprint = Printing(
    oracleId = "0fae1c73-fd34-433e-8989-fbd80e3b1c70",
    name = "Pincher Beetles",
    setCode = "10E",
    collectorNumber = "285",
    artist = "Stephen Daniele",
    imageUri = "https://cards.scryfall.io/normal/front/4/8/48484372-325a-4bef-ae86-a7f61816fd65.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
