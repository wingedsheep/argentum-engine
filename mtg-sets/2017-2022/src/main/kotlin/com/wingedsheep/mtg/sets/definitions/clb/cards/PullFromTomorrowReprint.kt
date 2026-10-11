package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Pull from Tomorrow reprint in CLB. The canonical CardDefinition lives in
 * Amonkhet (`akh`), the card's earliest real printing; this file
 * contributes only per-printing presentation data.
 */
val PullFromTomorrowReprint = Printing(
    oracleId = "b1a23235-3076-475c-a68a-db29cf2a9dba",
    name = "Pull from Tomorrow",
    setCode = "CLB",
    collectorNumber = "731",
    scryfallId = "d3d91bf6-25c7-4478-854a-e35290b6b049",
    artist = "Sara Winters",
    imageUri = "https://cards.scryfall.io/normal/front/d/3/d3d91bf6-25c7-4478-854a-e35290b6b049.jpg?1783922462",
    releaseDate = "2022-06-10",
    rarity = Rarity.RARE,
)
