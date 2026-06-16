package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Lord of Atlantis reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val LordOfAtlantisReprint = Printing(
    oracleId = "cc7f290f-ca00-4285-9bdb-4b4402444f30",
    name = "Lord of Atlantis",
    setCode = "LEB",
    collectorNumber = "63",
    artist = "Melissa A. Benson",
    imageUri = "https://cards.scryfall.io/normal/front/2/7/27d7ac1f-2243-4c70-95a4-2b7343c8d92d.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)
