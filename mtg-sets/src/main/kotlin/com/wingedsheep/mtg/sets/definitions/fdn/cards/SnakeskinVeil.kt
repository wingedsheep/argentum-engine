package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Snakeskin Veil reprint in FDN.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * KHM's `cards/` package (the card's earliest real printing). This file contributes
 * only the FDN-specific presentation row — set, collector number, art — picked up
 * automatically by `CardDiscovery.findPrintingsIn` and surfaced via the set's
 * `printings`.
 */
val SnakeskinVeilReprint = Printing(
    oracleId = "1e6a24be-8281-41c1-a5ba-b68f0ef1d7b8",
    name = "Snakeskin Veil",
    setCode = "FDN",
    collectorNumber = "233",
    scryfallId = "6cc4c21d-9bdc-4490-9203-17f51db0ddd1",
    artist = "Dan Murayama Scott",
    imageUri = "https://cards.scryfall.io/normal/front/6/c/6cc4c21d-9bdc-4490-9203-17f51db0ddd1.jpg?1730489471",
    releaseDate = "2024-11-15",
    rarity = Rarity.UNCOMMON,
)
