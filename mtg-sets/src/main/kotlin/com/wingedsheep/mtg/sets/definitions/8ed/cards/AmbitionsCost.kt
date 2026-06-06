package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Ambition's Cost reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * PTK's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val AmbitionsCostReprint = Printing(
    oracleId = "84de4fec-2f38-4293-93d3-b3882c5aac14",
    name = "Ambition's Cost",
    setCode = "8ED",
    collectorNumber = "118",
    artist = "Junko Taguchi",
    imageUri = "https://cards.scryfall.io/normal/front/7/0/705692ad-763b-42aa-8524-21dc32df3f10.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)
