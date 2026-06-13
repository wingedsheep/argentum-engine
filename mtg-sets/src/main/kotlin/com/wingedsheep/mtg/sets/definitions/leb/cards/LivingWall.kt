package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Living Wall reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val LivingWallReprint = Printing(
    oracleId = "4844312c-3c9d-4ca1-986d-4ad35e68454e",
    name = "Living Wall",
    setCode = "LEB",
    collectorNumber = "259",
    artist = "Anson Maddocks",
    imageUri = "https://cards.scryfall.io/normal/front/0/c/0c2cd1c8-8734-4534-ae92-def4d94ef5bc.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)
