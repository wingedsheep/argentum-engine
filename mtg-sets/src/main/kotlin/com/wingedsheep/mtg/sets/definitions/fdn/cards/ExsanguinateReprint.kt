package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Exsanguinate reprint in Foundations. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in SOM's `cards/` package (the card's
 * earliest real printing); this file contributes only the FDN presentation row.
 */
val ExsanguinateReprint = Printing(
    oracleId = "8164b1e8-3350-465e-8a17-75f57d326344",
    name = "Exsanguinate",
    setCode = "FDN",
    collectorNumber = "173",
    scryfallId = "f11d7311-4066-4a5d-ba28-9857fa707a0b",
    artist = "Marie Magny",
    imageUri = "https://cards.scryfall.io/normal/front/f/1/f11d7311-4066-4a5d-ba28-9857fa707a0b.jpg?1782689117",
    releaseDate = "2024-11-15",
    rarity = Rarity.UNCOMMON,
)
