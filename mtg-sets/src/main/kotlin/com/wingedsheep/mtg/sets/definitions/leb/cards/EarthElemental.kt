package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Earth Elemental reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val EarthElementalReprint = Printing(
    oracleId = "3c97c311-7ad5-47ec-b421-f6c3bfbda9fb",
    name = "Earth Elemental",
    setCode = "LEB",
    collectorNumber = "145",
    artist = "Dan Frazier",
    imageUri = "https://cards.scryfall.io/normal/front/c/4/c427e8cc-d908-4b88-931d-a540fc8bfe74.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)
