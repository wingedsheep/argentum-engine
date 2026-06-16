package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Tsunami reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val TsunamiReprint = Printing(
    oracleId = "ef2b1565-7b02-4cab-9031-beb1701ee929",
    name = "Tsunami",
    setCode = "LEB",
    collectorNumber = "222",
    artist = "Richard Thomas",
    imageUri = "https://cards.scryfall.io/normal/front/1/f/1f4b6f5a-1ba2-409d-9b9b-91e2c1470f62.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)
