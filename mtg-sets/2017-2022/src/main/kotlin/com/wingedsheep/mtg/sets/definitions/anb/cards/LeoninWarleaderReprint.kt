package com.wingedsheep.mtg.sets.definitions.anb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Leonin Warleader reprint in ANB. Canonical CardDefinition lives in Core Set 2019 (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.m19.cards.LeoninWarleader`.
 */
val LeoninWarleaderReprint = Printing(
    oracleId = "8b1351e6-165e-4ca3-96d5-4774b3176362",
    name = "Leonin Warleader",
    setCode = "ANB",
    collectorNumber = "13",
    scryfallId = "9387c4ae-1a45-4ea5-875a-5a6c1d6a7846",
    artist = "Jakub Kasper",
    imageUri = "https://cards.scryfall.io/normal/front/9/3/9387c4ae-1a45-4ea5-875a-5a6c1d6a7846.jpg?1783929842",
    releaseDate = "2020-08-13",
    rarity = Rarity.RARE,
)
