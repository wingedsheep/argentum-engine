package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Cathar Commando reprint in FDN.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MID's `cards/` package (the card's earliest real printing). This file contributes only
 * the FDN-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val CatharCommandoReprint = Printing(
    oracleId = "774dce79-67e0-4820-8013-c7a7347993ce",
    name = "Cathar Commando",
    setCode = "FDN",
    collectorNumber = "139",
    artist = "Evyn Fong",
    imageUri = "https://cards.scryfall.io/normal/front/1/9/19cf024d-edb6-4a79-8676-73f8db0cdf1f.jpg",
    releaseDate = "2024-11-15",
    rarity = Rarity.COMMON,
)
