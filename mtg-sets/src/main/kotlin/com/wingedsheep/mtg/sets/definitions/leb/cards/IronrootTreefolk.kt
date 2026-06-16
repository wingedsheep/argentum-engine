package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Ironroot Treefolk reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val IronrootTreefolkReprint = Printing(
    oracleId = "b7c0bb85-fb87-4c73-bc1b-7b4dc763c7e8",
    name = "Ironroot Treefolk",
    setCode = "LEB",
    collectorNumber = "204",
    artist = "Jesper Myrfors",
    imageUri = "https://cards.scryfall.io/normal/front/1/d/1d9479ae-2b42-4137-9e62-ef4d7fd17d0c.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)
