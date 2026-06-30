package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Broken Wings reprint in Foundations (FDN). The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in ZNR's `cards/` package; this file
 * contributes only presentation data.
 */
val BrokenWingsReprint = Printing(
    oracleId = "5e316864-d55c-496f-8f46-773567896864",
    name = "Broken Wings",
    setCode = "FDN",
    collectorNumber = "214",
    scryfallId = "61f9cbeb-cc9c-4562-be65-8a77053faefe",
    artist = "Svetlin Velinov",
    imageUri = "https://cards.scryfall.io/normal/front/6/1/61f9cbeb-cc9c-4562-be65-8a77053faefe.jpg?1782689083",
    releaseDate = "2024-11-15",
    rarity = Rarity.COMMON,
)
