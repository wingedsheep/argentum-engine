package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Femeref Archers reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MIR's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val FemerefArchersReprint = Printing(
    oracleId = "b5867796-5898-4a75-a75a-c802407ab338",
    name = "Femeref Archers",
    setCode = "10E",
    collectorNumber = "264",
    artist = "Zoltan Boros & Gabor Szikszai",
    imageUri = "https://cards.scryfall.io/normal/front/6/1/61df49d2-1324-460b-8c72-6b6fe8b03c15.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)
