package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Balduvian Barbarians reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ICE's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val BalduvianBarbariansReprint = Printing(
    oracleId = "23720758-f572-45e3-9bec-f6d3b974d7f5",
    name = "Balduvian Barbarians",
    setCode = "8ED",
    collectorNumber = "176",
    artist = "Jim Nelson",
    imageUri = "https://cards.scryfall.io/normal/front/4/6/46092f3f-7ef9-4967-b47b-40747a8cf37d.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
