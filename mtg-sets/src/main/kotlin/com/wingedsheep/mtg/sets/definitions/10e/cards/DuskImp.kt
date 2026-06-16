package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Dusk Imp reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ODY's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val DuskImpReprint = Printing(
    oracleId = "1389ae4a-c3a5-4678-9012-937a3cbaf7f7",
    name = "Dusk Imp",
    setCode = "10E",
    collectorNumber = "140",
    artist = "Pete Venters",
    imageUri = "https://cards.scryfall.io/normal/front/b/7/b7f264f6-832c-42fe-8642-00ee0f009c08.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
