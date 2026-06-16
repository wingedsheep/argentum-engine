package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Glasses of Urza reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val GlassesOfUrzaReprint = Printing(
    oracleId = "af7fabf4-8d55-4b06-9c21-472f4a5775b4",
    name = "Glasses of Urza",
    setCode = "LEB",
    collectorNumber = "246",
    artist = "Douglas Shuler",
    imageUri = "https://cards.scryfall.io/normal/front/e/b/eb6953fd-ee48-49dc-9c9c-bfb9a9dc06d0.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)
