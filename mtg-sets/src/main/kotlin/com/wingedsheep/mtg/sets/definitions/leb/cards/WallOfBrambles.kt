package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Wall of Brambles reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val WallOfBramblesReprint = Printing(
    oracleId = "f8d82a00-c10e-4b9e-9642-d05706900a97",
    name = "Wall of Brambles",
    setCode = "LEB",
    collectorNumber = "224",
    artist = "Anson Maddocks",
    imageUri = "https://cards.scryfall.io/normal/front/c/2/c2fca52b-80b3-4b6b-9a49-110c66557894.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)
