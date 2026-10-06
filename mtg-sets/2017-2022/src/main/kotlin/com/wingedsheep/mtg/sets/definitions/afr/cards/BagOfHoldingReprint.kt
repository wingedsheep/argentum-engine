package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Bag of Holding reprint in AFR. Canonical CardDefinition lives in Core Set 2020 (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.m20.cards.BagOfHolding`.
 */
val BagOfHoldingReprint = Printing(
    oracleId = "63c04040-e109-494e-baa6-c639a6c9a996",
    name = "Bag of Holding",
    setCode = "AFR",
    collectorNumber = "240",
    scryfallId = "6ea5e4e9-491b-4c80-8801-f4cd5225c601",
    artist = "Evyn Fong",
    imageUri = "https://cards.scryfall.io/normal/front/6/e/6ea5e4e9-491b-4c80-8801-f4cd5225c601.jpg?1783926440",
    releaseDate = "2021-07-23",
    rarity = Rarity.UNCOMMON,
)
