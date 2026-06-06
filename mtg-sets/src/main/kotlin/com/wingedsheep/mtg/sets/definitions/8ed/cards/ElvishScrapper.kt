package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Elvish Scrapper reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ONS's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ElvishScrapperReprint = Printing(
    oracleId = "81b2242b-40c9-475a-a198-28dd348235ec",
    name = "Elvish Scrapper",
    setCode = "8ED",
    collectorNumber = "245",
    artist = "Edward P. Beard, Jr.",
    imageUri = "https://cards.scryfall.io/normal/front/4/7/47ebab87-fd0c-43d0-bde1-e36065d764dd.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)
