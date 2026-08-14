package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Sculpting Steel reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in MRD's `cards/` package
 * (Mirrodin is the card's earliest real printing). This file contributes only the 10E-specific
 * presentation row — set, collector number, art.
 */
val SculptingSteelReprint = Printing(
    oracleId = "6c85271c-c711-49b0-a72e-9e576c33714d",
    name = "Sculpting Steel",
    setCode = "10E",
    collectorNumber = "342",
    scryfallId = "41368983-14f8-4efa-bb60-b0a7ceb3d021",
    artist = "Heather Hudson",
    imageUri = "https://cards.scryfall.io/normal/front/4/1/41368983-14f8-4efa-bb60-b0a7ceb3d021.jpg?1783942978",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)
