package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Sift reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * STH's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SiftReprint = Printing(
    oracleId = "6e060984-2015-4dc4-b53d-ab6bf2abdacc",
    name = "Sift",
    setCode = "10E",
    collectorNumber = "108",
    artist = "Jeremy Jarvis",
    imageUri = "https://cards.scryfall.io/normal/front/9/1/917a5c02-89b1-4d06-8d65-8db8c9c485a9.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
