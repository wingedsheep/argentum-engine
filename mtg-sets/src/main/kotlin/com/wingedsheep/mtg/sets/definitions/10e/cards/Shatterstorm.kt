package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Shatterstorm reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ATQ's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ShatterstormReprint = Printing(
    oracleId = "96ce2403-4607-440a-92ae-80aceb458c5d",
    name = "Shatterstorm",
    setCode = "10E",
    collectorNumber = "229",
    artist = "Paolo Parente",
    imageUri = "https://cards.scryfall.io/normal/front/f/7/f7a1aa93-26d1-40b0-82d8-414f56a36337.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)
