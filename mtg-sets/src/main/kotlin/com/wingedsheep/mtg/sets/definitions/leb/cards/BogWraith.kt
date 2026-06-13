package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Bog Wraith reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val BogWraithReprint = Printing(
    oracleId = "508248d1-09a4-4e41-a4c9-286618e5061e",
    name = "Bog Wraith",
    setCode = "LEB",
    collectorNumber = "96",
    artist = "Jeff A. Menges",
    imageUri = "https://cards.scryfall.io/normal/front/d/a/da26289f-e0e6-4aae-8782-ebdbabf39819.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)
