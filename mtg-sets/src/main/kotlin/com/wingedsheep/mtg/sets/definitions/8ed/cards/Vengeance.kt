package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Vengeance reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * POR's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val VengeanceReprint = Printing(
    oracleId = "1d001145-5d14-43a9-bf3b-3ce5c20b2a46",
    name = "Vengeance",
    setCode = "8ED",
    collectorNumber = "S2",
    artist = "Paolo Parente",
    imageUri = "https://cards.scryfall.io/normal/front/1/5/15a680ae-aa4f-4030-8032-113b2b80e1fb.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)
