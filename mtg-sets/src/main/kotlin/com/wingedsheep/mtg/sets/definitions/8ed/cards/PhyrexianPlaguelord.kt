package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Phyrexian Plaguelord reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ULG's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val PhyrexianPlaguelordReprint = Printing(
    oracleId = "aff9e844-9e03-490b-b44f-10d385738cc6",
    name = "Phyrexian Plaguelord",
    setCode = "8ED",
    collectorNumber = "153",
    artist = "Kev Walker",
    imageUri = "https://cards.scryfall.io/normal/front/2/8/284f9e91-9318-44d9-995b-8c00752a3b01.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.RARE,
)
