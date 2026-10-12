package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Curate reprint in BRO.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types) lives in the STX
 * `cards/` package (its earliest printing). This file contributes only the BRO-specific
 * presentation row — set, collector number, art — picked up automatically by
 * `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val CurateReprint = Printing(
    oracleId = "75e8907d-f726-4a63-ba8b-c0fd4936734e",
    name = "Curate",
    setCode = "BRO",
    collectorNumber = "44",
    scryfallId = "fe8c3fc8-c1cc-4dfc-94cb-1538bff9d09a",
    artist = "Ryan Valle",
    imageUri = "https://cards.scryfall.io/normal/front/f/e/fe8c3fc8-c1cc-4dfc-94cb-1538bff9d09a.jpg?1783920114",
    releaseDate = "2022-11-18",
    rarity = Rarity.COMMON,
)
