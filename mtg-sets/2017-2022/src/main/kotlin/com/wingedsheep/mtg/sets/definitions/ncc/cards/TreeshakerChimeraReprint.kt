package com.wingedsheep.mtg.sets.definitions.ncc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Treeshaker Chimera reprint in New Capenna Commander. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in Theros Beyond Death's `cards/` package; this
 * file contributes only the New Capenna Commander presentation row.
 */
val TreeshakerChimeraReprint = Printing(
    oracleId = "8b119b69-4794-443e-aa13-747c125b5b1b",
    name = "Treeshaker Chimera",
    setCode = "NCC",
    collectorNumber = "318",
    scryfallId = "5f73b249-be19-4a51-8759-648a2f122e6d",
    artist = "Vincent Proce",
    imageUri = "https://cards.scryfall.io/normal/front/5/f/5f73b249-be19-4a51-8759-648a2f122e6d.jpg?1783923238",
    releaseDate = "2022-04-29",
    rarity = Rarity.RARE,
)
