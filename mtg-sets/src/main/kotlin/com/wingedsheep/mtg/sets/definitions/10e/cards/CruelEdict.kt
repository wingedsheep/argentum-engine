package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Cruel Edict reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * P02's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val CruelEdictReprint = Printing(
    oracleId = "10c585c4-bf5b-4d8f-94a9-e9a5036a688f",
    name = "Cruel Edict",
    setCode = "10E",
    collectorNumber = "133",
    artist = "Michael Sutfin",
    imageUri = "https://cards.scryfall.io/normal/front/0/c/0c3d9c1d-a984-4971-810c-e493d0a55e5c.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)
