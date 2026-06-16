package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Zombie Master reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ZombieMasterReprint = Printing(
    oracleId = "5446c92f-ff22-4e9b-a2f6-e64c8560c1e0",
    name = "Zombie Master",
    setCode = "LEB",
    collectorNumber = "138",
    artist = "Jeff A. Menges",
    imageUri = "https://cards.scryfall.io/normal/front/a/1/a1bfda92-b932-46d8-b549-e2bc2b584a17.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)
