package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Soul Feast reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * UDS's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SoulFeastReprint = Printing(
    oracleId = "8186fd80-015f-470c-9e1c-cbf45764a057",
    name = "Soul Feast",
    setCode = "8ED",
    collectorNumber = "165",
    artist = "Adam Rex",
    imageUri = "https://cards.scryfall.io/normal/front/a/6/a6b341b3-88d3-4ddc-8fa8-b15596b1b475.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)
