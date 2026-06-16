package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Shanodin Dryads reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ShanodinDryadsReprint = Printing(
    oracleId = "998484cc-fefc-4da5-9987-5d6e89599c34",
    name = "Shanodin Dryads",
    setCode = "LEB",
    collectorNumber = "217",
    artist = "Anson Maddocks",
    imageUri = "https://cards.scryfall.io/normal/front/1/a/1ac8bdb0-2dfd-4531-a4d9-420f2f2a90be.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)
