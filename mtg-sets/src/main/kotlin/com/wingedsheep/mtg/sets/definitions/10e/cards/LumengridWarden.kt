package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Lumengrid Warden reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MRD's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val LumengridWardenReprint = Printing(
    oracleId = "ee16a8f3-2522-4c36-b4de-65484dfb7455",
    name = "Lumengrid Warden",
    setCode = "10E",
    collectorNumber = "89",
    artist = "Francis Tsai",
    imageUri = "https://cards.scryfall.io/normal/front/0/a/0a03ba5a-ac27-4fce-9eaf-b029ab26f9e1.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
