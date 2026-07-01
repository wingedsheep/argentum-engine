package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Bake into a Pie reprint in Foundations. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in ELD's `cards/` package (the card's
 * earliest real printing); this file contributes only the FDN presentation row.
 */
val BakeIntoAPieReprint = Printing(
    oracleId = "1b9ec782-0ba1-41f1-bc39-d3302494ecb3",
    name = "Bake into a Pie",
    setCode = "FDN",
    collectorNumber = "169",
    scryfallId = "2ab0e660-86a3-4b92-82fa-77dcb5db947d",
    artist = "Zoltan Boros",
    imageUri = "https://cards.scryfall.io/normal/front/2/a/2ab0e660-86a3-4b92-82fa-77dcb5db947d.jpg?1782689121",
    releaseDate = "2024-11-15",
    rarity = Rarity.COMMON,
)
