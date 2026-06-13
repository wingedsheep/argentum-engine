package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Birds of Paradise reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val BirdsOfParadiseReprint = Printing(
    oracleId = "d3a0b660-358c-41bd-9cd2-41fbf3491b1a",
    name = "Birds of Paradise",
    setCode = "LEB",
    collectorNumber = "187",
    artist = "Mark Poole",
    imageUri = "https://cards.scryfall.io/normal/front/8/5/852d7a68-8682-4073-a44b-f10f5613879c.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)
