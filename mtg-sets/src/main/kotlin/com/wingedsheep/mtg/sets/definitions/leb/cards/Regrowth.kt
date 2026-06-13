package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Regrowth reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val RegrowthReprint = Printing(
    oracleId = "e6e4a8bd-5c40-4654-8de1-0da9afed90fd",
    name = "Regrowth",
    setCode = "LEB",
    collectorNumber = "215",
    artist = "Dameon Willich",
    imageUri = "https://cards.scryfall.io/normal/front/8/9/898cd314-9060-4f1c-a821-1d61a292a12b.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)
