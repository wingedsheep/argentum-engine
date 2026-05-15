package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Evolving Wilds reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ROE's `cards/` package (the card's earliest real printing). This file contributes
 * only the INR-specific presentation row — set, collector number, art — picked up
 * automatically by `CardDiscovery.findPrintingsIn` and surfaced via the set's
 * `printings`.
 */
val EvolvingWildsReprint = Printing(
    oracleId = "a75445d3-1303-4bb5-89ad-26ea93fecd48",
    name = "Evolving Wilds",
    setCode = "INR",
    collectorNumber = "278",
    scryfallId = "e3056678-4ef0-4847-af08-df218ef5fb2b",
    artist = "Cliff Childs",
    imageUri = "https://cards.scryfall.io/normal/front/e/3/e3056678-4ef0-4847-af08-df218ef5fb2b.jpg?1736468674",
    releaseDate = "2025-01-24",
    rarity = Rarity.COMMON,
)
