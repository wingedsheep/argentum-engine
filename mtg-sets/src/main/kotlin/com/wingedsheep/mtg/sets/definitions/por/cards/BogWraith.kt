package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Bog Wraith reprint in POR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes
 * only the POR-specific presentation row — set, collector number, art — picked up
 * automatically by `CardDiscovery.findPrintingsIn` and surfaced via the set's
 * `printings`.
 */
val BogWraithReprint = Printing(
    oracleId = "508248d1-09a4-4e41-a4c9-286618e5061e",
    name = "Bog Wraith",
    setCode = "POR",
    collectorNumber = "83",
    artist = "Jeff A. Menges",
    imageUri = "https://cards.scryfall.io/normal/front/4/4/4487d7d0-d5a5-4b0c-bf30-e0ec511e9aa4.jpg",
    releaseDate = "1997-05-01",
    rarity = Rarity.UNCOMMON,
)
