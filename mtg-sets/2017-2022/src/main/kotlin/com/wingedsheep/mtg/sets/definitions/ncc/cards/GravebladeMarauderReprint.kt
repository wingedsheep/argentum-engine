package com.wingedsheep.mtg.sets.definitions.ncc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Graveblade Marauder reprint in New Capenna Commander. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in Magic Origins' `cards/` package; this
 * file contributes only the New Capenna Commander presentation row.
 */
val GravebladeMarauderReprint = Printing(
    oracleId = "33f29fcb-c9ea-4d81-afa2-5c9575b4d58e",
    name = "Graveblade Marauder",
    setCode = "NCC",
    collectorNumber = "251",
    scryfallId = "188692aa-a326-43c9-845b-9fdc17a05d55",
    artist = "Jason Rainville",
    imageUri = "https://cards.scryfall.io/normal/front/1/8/188692aa-a326-43c9-845b-9fdc17a05d55.jpg?1783923267",
    releaseDate = "2022-04-29",
    rarity = Rarity.RARE,
)
