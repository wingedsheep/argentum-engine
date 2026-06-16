package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Control Magic reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ControlMagicReprint = Printing(
    oracleId = "cd0d7141-46d2-4aa3-bc77-6b3b4513803e",
    name = "Control Magic",
    setCode = "LEB",
    collectorNumber = "53",
    artist = "Dameon Willich",
    imageUri = "https://cards.scryfall.io/normal/front/1/3/133315bd-3c46-4eff-938e-4dba63631c1b.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)
