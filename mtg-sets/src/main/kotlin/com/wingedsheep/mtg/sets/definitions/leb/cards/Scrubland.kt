package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Scrubland reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ScrublandReprint = Printing(
    oracleId = "c8d95ca8-7d12-4072-aeaf-e20f248c7e39",
    name = "Scrubland",
    setCode = "LEB",
    collectorNumber = "282",
    artist = "Jesper Myrfors",
    imageUri = "https://cards.scryfall.io/normal/front/8/c/8cf99186-3167-4092-8efb-e7448609ceba.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)
