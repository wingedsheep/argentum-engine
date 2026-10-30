package com.wingedsheep.mtg.sets.definitions.cns.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Wrap in Flames reprint in CNS. The canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in another set's `cards/` package; this file contributes only presentation data.
 */
val WrapInFlamesReprint = Printing(
    oracleId = "b46efd59-eda7-4fe6-ace7-67ca8f6c088d",
    name = "Wrap in Flames",
    setCode = "CNS",
    collectorNumber = "158",
    scryfallId = "bbb77957-dcc2-4732-a766-091fcbbfaa9d",
    artist = "Véronique Meignaud",
    imageUri = "https://cards.scryfall.io/normal/front/b/b/bbb77957-dcc2-4732-a766-091fcbbfaa9d.jpg?1783939346",
    releaseDate = "2014-06-06",
    rarity = Rarity.COMMON,
)
