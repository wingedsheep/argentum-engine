package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Wrap in Flames reprint in THB. The canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in another set's `cards/` package; this file contributes only presentation data.
 */
val WrapInFlamesReprint = Printing(
    oracleId = "b46efd59-eda7-4fe6-ace7-67ca8f6c088d",
    name = "Wrap in Flames",
    setCode = "THB",
    collectorNumber = "164",
    scryfallId = "b5f7ff4f-3fd6-4084-aaf7-9f290ecc0181",
    artist = "Kari Christensen",
    imageUri = "https://cards.scryfall.io/normal/front/b/5/b5f7ff4f-3fd6-4084-aaf7-9f290ecc0181.jpg?1783931542",
    releaseDate = "2020-01-24",
    rarity = Rarity.COMMON,
)
