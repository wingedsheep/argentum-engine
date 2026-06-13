package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Counterspell reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val CounterspellReprint = Printing(
    oracleId = "cc187110-1148-4090-bbb8-e205694a39f5",
    name = "Counterspell",
    setCode = "LEB",
    collectorNumber = "55",
    artist = "Mark Poole",
    imageUri = "https://cards.scryfall.io/normal/front/9/e/9e11bf7c-f439-4529-b29a-d711359807ef.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)
