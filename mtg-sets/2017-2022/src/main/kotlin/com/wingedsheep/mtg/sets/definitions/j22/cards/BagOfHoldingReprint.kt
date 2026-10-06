package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Bag of Holding reprint in J22. Canonical CardDefinition lives in Core Set 2020 (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.m20.cards.BagOfHolding`.
 */
val BagOfHoldingReprint = Printing(
    oracleId = "63c04040-e109-494e-baa6-c639a6c9a996",
    name = "Bag of Holding",
    setCode = "J22",
    collectorNumber = "756",
    scryfallId = "23e799bd-87c7-4f62-87a9-6f8f8d459db8",
    artist = "Dmitry Burmak",
    imageUri = "https://cards.scryfall.io/normal/front/2/3/23e799bd-87c7-4f62-87a9-6f8f8d459db8.jpg?1783918818",
    releaseDate = "2022-12-02",
    rarity = Rarity.RARE,
)
