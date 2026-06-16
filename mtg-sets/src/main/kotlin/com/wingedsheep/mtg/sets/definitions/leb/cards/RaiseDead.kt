package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Raise Dead reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val RaiseDeadReprint = Printing(
    oracleId = "cbc9c731-181a-4f00-a7b0-eb7e56eac2ea",
    name = "Raise Dead",
    setCode = "LEB",
    collectorNumber = "123",
    artist = "Jeff A. Menges",
    imageUri = "https://cards.scryfall.io/normal/front/0/0/0066c7a6-7775-43ba-81cd-35fbc5621bc3.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)
