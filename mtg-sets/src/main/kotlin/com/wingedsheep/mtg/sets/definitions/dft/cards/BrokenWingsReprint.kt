package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Broken Wings reprint in DFT. The canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in another set's `cards/` package; this file contributes only presentation data.
 */
val BrokenWingsReprint = Printing(
    oracleId = "5e316864-d55c-496f-8f46-773567896864",
    name = "Broken Wings",
    setCode = "DFT",
    collectorNumber = "156",
    scryfallId = "1d7d5b71-7c1b-4fc2-a5ec-7285e17ffe0f",
    artist = "Nils Hamm",
    imageUri = "https://cards.scryfall.io/normal/front/1/d/1d7d5b71-7c1b-4fc2-a5ec-7285e17ffe0f.jpg?1782687839",
    releaseDate = "2025-02-14",
    rarity = Rarity.COMMON,
)
