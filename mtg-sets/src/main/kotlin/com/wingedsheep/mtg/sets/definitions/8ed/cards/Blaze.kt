package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Blaze reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * POR's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val BlazeReprint = Printing(
    oracleId = "0596920f-9946-42f4-a03b-24aab67f9f1b",
    name = "Blaze",
    setCode = "8ED",
    collectorNumber = "177",
    artist = "Alex Horley-Orlandelli",
    imageUri = "https://cards.scryfall.io/normal/front/5/d/5de4525e-6c9d-4396-9280-6fed9802123a.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)
