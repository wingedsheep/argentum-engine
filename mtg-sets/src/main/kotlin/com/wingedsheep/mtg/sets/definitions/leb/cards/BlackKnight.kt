package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Black Knight reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val BlackKnightReprint = Printing(
    oracleId = "9456c5b6-946d-403a-8ed0-dff9f921d98c",
    name = "Black Knight",
    setCode = "LEB",
    collectorNumber = "95",
    artist = "Jeff A. Menges",
    imageUri = "https://cards.scryfall.io/normal/front/1/e/1eced352-d49c-4e91-a368-52904d77a69d.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)
