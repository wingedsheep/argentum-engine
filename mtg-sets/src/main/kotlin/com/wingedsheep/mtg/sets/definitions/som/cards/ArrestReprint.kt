package com.wingedsheep.mtg.sets.definitions.som.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Arrest reprint in SOM. Canonical CardDefinition lives in Mercadian Masques (its earliest
 * real printing), `com.wingedsheep.mtg.sets.definitions.mmq.cards.Arrest`.
 */
val ArrestReprint = Printing(
    oracleId = "81728b98-8cf9-4734-a318-69184bb4d15c",
    name = "Arrest",
    setCode = "SOM",
    collectorNumber = "2",
    scryfallId = "f52d6cf9-1d92-4d3b-8631-0db19a073b44",
    artist = "Daarken",
    imageUri = "https://cards.scryfall.io/normal/front/f/5/f52d6cf9-1d92-4d3b-8631-0db19a073b44.jpg?1783941747",
    releaseDate = "2010-10-01",
    rarity = Rarity.COMMON,
)
