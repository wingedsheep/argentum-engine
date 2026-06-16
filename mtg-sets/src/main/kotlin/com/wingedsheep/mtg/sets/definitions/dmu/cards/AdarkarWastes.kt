package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Adarkar Wastes reprint in DMU.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in ICE's `cards/` package
 * (the card's earliest real printing). This file contributes only the DMU-specific
 * presentation row, surfaced via the set's `printings`.
 */
val AdarkarWastesReprint = Printing(
    oracleId = "d5ad26cc-2bdb-46b7-b8bf-dd099d5fa09b",
    name = "Adarkar Wastes",
    setCode = "DMU",
    collectorNumber = "243",
    artist = "Piotr Dura",
    imageUri = "https://cards.scryfall.io/normal/front/0/8/08ae1037-6f70-41a9-b75e-98fa9a2152c8.jpg",
    releaseDate = "2022-09-09",
    rarity = Rarity.RARE,
)
