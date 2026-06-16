package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Prodigal Sorcerer reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ProdigalSorcererReprint = Printing(
    oracleId = "5e961d15-5972-4e4b-9385-1cd7cd7c6bbe",
    name = "Prodigal Sorcerer",
    setCode = "LEB",
    collectorNumber = "74",
    artist = "Douglas Shuler",
    imageUri = "https://cards.scryfall.io/normal/front/c/4/c420abf2-05ec-4623-8a6c-353736a4edeb.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)
