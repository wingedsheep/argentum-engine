package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Conservator reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ConservatorReprint = Printing(
    oracleId = "1940e56d-0972-4ca4-946c-bbd42dde1dcb",
    name = "Conservator",
    setCode = "LEB",
    collectorNumber = "238",
    artist = "Amy Weber",
    imageUri = "https://cards.scryfall.io/normal/front/d/4/d4f54af3-7c85-43da-b0ce-df4a44af4736.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)
