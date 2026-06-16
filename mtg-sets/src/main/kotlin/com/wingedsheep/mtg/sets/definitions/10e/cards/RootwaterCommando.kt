package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Rootwater Commando reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * NEM's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val RootwaterCommandoReprint = Printing(
    oracleId = "d62e9510-dc41-493f-8289-6e11303884fc",
    name = "Rootwater Commando",
    setCode = "10E",
    collectorNumber = "102",
    artist = "Mark Tedin",
    imageUri = "https://cards.scryfall.io/normal/front/1/d/1da47bd4-58b6-4ef3-bd42-caf947e38645.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
