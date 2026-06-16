package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Sinkhole reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SinkholeReprint = Printing(
    oracleId = "5a46ad2a-35b1-4dd5-b7c3-fec36b7c67ab",
    name = "Sinkhole",
    setCode = "LEB",
    collectorNumber = "130",
    artist = "Sandra Everingham",
    imageUri = "https://cards.scryfall.io/normal/front/5/2/52ea4387-f23c-430c-99d6-0248a4ab1713.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)
