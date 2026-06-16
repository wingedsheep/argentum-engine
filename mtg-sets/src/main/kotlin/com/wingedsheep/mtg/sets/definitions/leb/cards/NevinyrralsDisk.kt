package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Nevinyrral's Disk reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val NevinyrralsDiskReprint = Printing(
    oracleId = "96230edf-568a-47dd-b877-9d92aa58fac8",
    name = "Nevinyrral's Disk",
    setCode = "LEB",
    collectorNumber = "267",
    artist = "Mark Tedin",
    imageUri = "https://cards.scryfall.io/normal/front/d/b/dbb21f21-668a-4d57-8d05-8db11fb82d99.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)
