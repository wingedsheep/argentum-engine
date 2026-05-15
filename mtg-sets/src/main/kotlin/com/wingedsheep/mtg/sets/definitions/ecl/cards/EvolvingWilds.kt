package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Evolving Wilds reprint in ECL.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ROE's `cards/` package (the card's earliest real printing). This file contributes
 * only the ECL-specific presentation row — set, collector number, art — picked up
 * automatically by `CardDiscovery.findPrintingsIn` and surfaced via the set's
 * `printings`.
 */
val EvolvingWildsReprint = Printing(
    oracleId = "a75445d3-1303-4bb5-89ad-26ea93fecd48",
    name = "Evolving Wilds",
    setCode = "ECL",
    collectorNumber = "264",
    scryfallId = "8c632984-5176-4c37-91df-6577cc294b85",
    artist = "Alayna Danner",
    imageUri = "https://cards.scryfall.io/normal/front/8/c/8c632984-5176-4c37-91df-6577cc294b85.jpg?1767863461",
    releaseDate = "2026-01-23",
    rarity = Rarity.COMMON,
)
