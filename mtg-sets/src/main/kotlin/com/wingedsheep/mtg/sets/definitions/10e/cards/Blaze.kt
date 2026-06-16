package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Blaze reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * POR's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val BlazeReprint = Printing(
    oracleId = "0596920f-9946-42f4-a03b-24aab67f9f1b",
    name = "Blaze",
    setCode = "10E",
    collectorNumber = "190",
    artist = "Alex Horley-Orlandelli",
    imageUri = "https://cards.scryfall.io/normal/front/2/1/213035f9-40f5-4b4e-88b3-e262ad684294.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)
