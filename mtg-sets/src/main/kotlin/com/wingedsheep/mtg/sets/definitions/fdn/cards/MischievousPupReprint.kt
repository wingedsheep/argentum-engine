package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Mischievous Pup reprint in Foundations. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in LCI's `cards/` package (the card's
 * earliest real printing); this file contributes only the FDN presentation row.
 */
val MischievousPupReprint = Printing(
    oracleId = "e75b187d-0e3b-4d94-a940-8b7e4e7ed1ca",
    name = "Mischievous Pup",
    setCode = "FDN",
    collectorNumber = "144",
    scryfallId = "7214d984-6400-44d7-bde6-57d96b606e78",
    artist = "Devin Platts",
    imageUri = "https://cards.scryfall.io/normal/front/7/2/7214d984-6400-44d7-bde6-57d96b606e78.jpg?1782689143",
    releaseDate = "2024-11-15",
    rarity = Rarity.UNCOMMON,
)
