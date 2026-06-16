package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Resurrection reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ResurrectionReprint = Printing(
    oracleId = "837417d8-8260-486d-a3ed-3b5711eaf34a",
    name = "Resurrection",
    setCode = "LEB",
    collectorNumber = "35",
    artist = "Dan Frazier",
    imageUri = "https://cards.scryfall.io/normal/front/5/0/50e3c741-5095-48a6-bd93-b9c4db265004.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)
