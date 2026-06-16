package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Phyrexian Rager reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * APC's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val PhyrexianRagerReprint = Printing(
    oracleId = "e409c9be-0c9a-43c3-adb4-8c47afc1d551",
    name = "Phyrexian Rager",
    setCode = "10E",
    collectorNumber = "167",
    artist = "Mark Tedin",
    imageUri = "https://cards.scryfall.io/normal/front/4/a/4a85419a-7ebb-4fe1-891f-b5295e18878f.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
