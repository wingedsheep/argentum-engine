package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Lifeforce reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val LifeforceReprint = Printing(
    oracleId = "07ae1fe5-5c3e-4d94-b809-8defd2ef44e3",
    name = "Lifeforce",
    setCode = "LEB",
    collectorNumber = "207",
    artist = "Dameon Willich",
    imageUri = "https://cards.scryfall.io/normal/front/3/7/3715abe2-5a8e-4bf4-ac02-6c755d86bb4c.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)
