package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Orcish Oriflamme reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val OrcishOriflammeReprint = Printing(
    oracleId = "0b16a650-68b0-44dc-a9e1-15b7966e0b18",
    name = "Orcish Oriflamme",
    setCode = "LEB",
    collectorNumber = "167",
    artist = "Dan Frazier",
    imageUri = "https://cards.scryfall.io/normal/front/f/2/f2752cf2-9a48-49a8-98ff-2e32a9121d78.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)
