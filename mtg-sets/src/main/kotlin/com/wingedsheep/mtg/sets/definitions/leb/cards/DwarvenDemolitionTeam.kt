package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Dwarven Demolition Team reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val DwarvenDemolitionTeamReprint = Printing(
    oracleId = "caf3c6ec-d17e-497c-8fe7-6f818ce93f96",
    name = "Dwarven Demolition Team",
    setCode = "LEB",
    collectorNumber = "143",
    artist = "Kev Brockschmidt",
    imageUri = "https://cards.scryfall.io/normal/front/e/5/e552dfb6-b8a5-419d-b098-5aedc0500684.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)
