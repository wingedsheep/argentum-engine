package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Dead Weight reprint in LCI.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ISD's `cards/` package (the card's earliest real printing). This file contributes only
 * the LCI-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val DeadWeightReprint = Printing(
    oracleId = "b1804304-fac1-4b19-a48d-6ade9407972a",
    name = "Dead Weight",
    setCode = "LCI",
    collectorNumber = "99",
    artist = "Javier Charro",
    imageUri = "https://cards.scryfall.io/normal/front/8/2/82e6b971-3f5b-47e7-8209-98d72ee781fc.jpg",
    releaseDate = "2023-11-17",
    rarity = Rarity.COMMON,
)
