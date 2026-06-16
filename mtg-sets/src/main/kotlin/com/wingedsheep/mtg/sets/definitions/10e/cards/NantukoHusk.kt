package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Nantuko Husk reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ONS's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val NantukoHuskReprint = Printing(
    oracleId = "0dcefc00-9425-43e3-bd47-1e34fff8b0e2",
    name = "Nantuko Husk",
    setCode = "10E",
    collectorNumber = "162",
    artist = "Carl Critchlow",
    imageUri = "https://cards.scryfall.io/normal/front/7/a/7a8f8f83-65bb-4da3-8263-e58162ebb24b.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)
