package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Green Ward reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val GreenWardReprint = Printing(
    oracleId = "727ab7f2-741e-4442-b5cb-e3032549fa87",
    name = "Green Ward",
    setCode = "LEB",
    collectorNumber = "21",
    artist = "Dan Frazier",
    imageUri = "https://cards.scryfall.io/normal/front/a/4/a488ce63-1adb-4051-9521-703bad8d02f6.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)
