package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Crusade reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val CrusadeReprint = Printing(
    oracleId = "4692740f-be90-459f-8d90-c4ae71771595",
    name = "Crusade",
    setCode = "LEB",
    collectorNumber = "17",
    artist = "Mark Poole",
    imageUri = "https://cards.scryfall.io/normal/front/2/d/2d5fbd9d-48bf-4600-8ca4-2ce2ca48128e.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)
