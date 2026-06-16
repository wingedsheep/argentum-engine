package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Birds of Paradise reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val BirdsOfParadiseReprint = Printing(
    oracleId = "d3a0b660-358c-41bd-9cd2-41fbf3491b1a",
    name = "Birds of Paradise",
    setCode = "10E",
    collectorNumber = "252",
    artist = "Marcelo Vignali",
    imageUri = "https://cards.scryfall.io/normal/front/4/4/44060205-8a5c-4f6d-8190-e65d5250935a.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)
