package com.wingedsheep.mtg.sets.definitions.bbd.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Wrap in Flames reprint in BBD. The canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in another set's `cards/` package; this file contributes only presentation data.
 */
val WrapInFlamesReprint = Printing(
    oracleId = "b46efd59-eda7-4fe6-ace7-67ca8f6c088d",
    name = "Wrap in Flames",
    setCode = "BBD",
    collectorNumber = "188",
    scryfallId = "c059d868-2e4a-466b-9faa-1be76a3a0378",
    artist = "Véronique Meignaud",
    imageUri = "https://cards.scryfall.io/normal/front/c/0/c059d868-2e4a-466b-9faa-1be76a3a0378.jpg?1783934808",
    releaseDate = "2018-06-08",
    rarity = Rarity.COMMON,
)
