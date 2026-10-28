package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Thwart the Grave reprint in CLB. The canonical CardDefinition lives in
 * Zendikar Rising (`znr`), the card's earliest real printing; this file
 * contributes only per-printing presentation data.
 */
val ThwartTheGraveReprint = Printing(
    oracleId = "b545e26f-2034-409c-80cc-51b72e75e6e4",
    name = "Thwart the Grave",
    setCode = "CLB",
    collectorNumber = "773",
    scryfallId = "cc339d3b-9c89-4679-bc83-8399d9a864c7",
    artist = "Wylie Beckert",
    imageUri = "https://cards.scryfall.io/normal/front/c/c/cc339d3b-9c89-4679-bc83-8399d9a864c7.jpg?1783922439",
    releaseDate = "2022-06-10",
    rarity = Rarity.UNCOMMON,
)
