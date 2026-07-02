package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Chart a Course reprint in The Lost Caverns of Ixalan (LCI). The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in Ixalan's `cards/` package;
 * this file contributes only the LCI presentation row.
 */
val ChartACourseReprint = Printing(
    oracleId = "05878e49-93ad-4144-9c50-a0bb86126c2e",
    name = "Chart a Course",
    setCode = "LCI",
    collectorNumber = "48",
    scryfallId = "233beaac-37a4-4824-8f31-438b6bfe794b",
    artist = "Josu Solano",
    imageUri = "https://cards.scryfall.io/normal/front/2/3/233beaac-37a4-4824-8f31-438b6bfe794b.jpg?1782694572",
    releaseDate = "2023-11-17",
    rarity = Rarity.UNCOMMON,
)
