package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Fyndhorn Elder reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ICE's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val FyndhornElderReprint = Printing(
    oracleId = "507bdb1a-90b4-4fd3-a1a3-e8be316e97f3",
    name = "Fyndhorn Elder",
    setCode = "8ED",
    collectorNumber = "251",
    artist = "Donato Giancola",
    imageUri = "https://cards.scryfall.io/normal/front/8/1/81c125cd-ea49-4511-a78c-42c1f7ce802d.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)
