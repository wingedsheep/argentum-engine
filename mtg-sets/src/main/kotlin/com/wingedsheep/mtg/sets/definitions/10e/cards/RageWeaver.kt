package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Rage Weaver reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * INV's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val RageWeaverReprint = Printing(
    oracleId = "9777a54b-1c60-4d6b-bb83-c57cb3a1d7e6",
    name = "Rage Weaver",
    setCode = "10E",
    collectorNumber = "223",
    artist = "John Matson",
    imageUri = "https://cards.scryfall.io/normal/front/5/7/57cec628-5f6a-4b42-9044-3f9a80cac3e6.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)
