package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Verduran Enchantress reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val VerduranEnchantressReprint = Printing(
    oracleId = "cd98a31b-cc7e-43f9-982e-109ad9850908",
    name = "Verduran Enchantress",
    setCode = "LEB",
    collectorNumber = "223",
    artist = "Kev Brockschmidt",
    imageUri = "https://cards.scryfall.io/normal/front/d/a/da3f051c-6be3-4f92-8f66-9f72d75dbcf5.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)
