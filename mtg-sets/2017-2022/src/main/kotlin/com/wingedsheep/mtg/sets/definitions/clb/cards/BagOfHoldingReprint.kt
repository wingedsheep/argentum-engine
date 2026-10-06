package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Bag of Holding reprint in CLB. Canonical CardDefinition lives in Core Set 2020 (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.m20.cards.BagOfHolding`.
 */
val BagOfHoldingReprint = Printing(
    oracleId = "63c04040-e109-494e-baa6-c639a6c9a996",
    name = "Bag of Holding",
    setCode = "CLB",
    collectorNumber = "299",
    scryfallId = "290f4b4f-4f48-4031-b62a-91a0d6716ddd",
    artist = "Evyn Fong",
    imageUri = "https://cards.scryfall.io/normal/front/2/9/290f4b4f-4f48-4031-b62a-91a0d6716ddd.jpg?1783922681",
    releaseDate = "2022-06-10",
    rarity = Rarity.UNCOMMON,
)
