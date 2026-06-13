package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Mox Ruby reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val MoxRubyReprint = Printing(
    oracleId = "ed85fa82-e4fa-434b-92a8-36b6075708d1",
    name = "Mox Ruby",
    setCode = "LEB",
    collectorNumber = "265",
    artist = "Dan Frazier",
    imageUri = "https://cards.scryfall.io/normal/front/f/d/fdac742b-16db-4e03-be8f-c600dbd522d5.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)
