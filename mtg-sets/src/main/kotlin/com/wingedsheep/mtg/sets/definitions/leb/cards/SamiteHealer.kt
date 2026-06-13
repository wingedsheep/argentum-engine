package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Samite Healer reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SamiteHealerReprint = Printing(
    oracleId = "95a0ca48-d924-47f4-86ed-42c673ee778c",
    name = "Samite Healer",
    setCode = "LEB",
    collectorNumber = "38",
    artist = "Tom Wänerstrand",
    imageUri = "https://cards.scryfall.io/normal/front/3/f/3fbfb106-29d8-4065-b306-51dba0ed11a4.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)
