package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Troll Ascetic reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MRD's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val TrollAsceticReprint = Printing(
    oracleId = "0585ea6a-ef78-4386-a22c-6b8d8fc6c03c",
    name = "Troll Ascetic",
    setCode = "10E",
    collectorNumber = "305",
    artist = "Puddnhead",
    imageUri = "https://cards.scryfall.io/normal/front/d/4/d447186e-62a7-4767-bb85-6439fd795350.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)
