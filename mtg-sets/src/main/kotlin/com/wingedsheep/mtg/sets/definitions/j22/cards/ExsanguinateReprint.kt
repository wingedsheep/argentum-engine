package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Exsanguinate reprint in Jumpstart 2022. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in SOM's `cards/` package (the card's
 * earliest real printing); this file contributes only the J22 presentation row.
 */
val ExsanguinateReprint = Printing(
    oracleId = "8164b1e8-3350-465e-8a17-75f57d326344",
    name = "Exsanguinate",
    setCode = "J22",
    collectorNumber = "413",
    scryfallId = "25ffebc3-8ad4-42da-b4f2-837ae22dee72",
    artist = "Carl Critchlow",
    imageUri = "https://cards.scryfall.io/normal/front/2/5/25ffebc3-8ad4-42da-b4f2-837ae22dee72.jpg?1782699098",
    releaseDate = "2022-12-02",
    rarity = Rarity.UNCOMMON,
)
