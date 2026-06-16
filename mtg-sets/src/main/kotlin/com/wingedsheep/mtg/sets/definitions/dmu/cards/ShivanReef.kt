package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Shivan Reef reprint in DMU.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in APC's `cards/` package
 * (the card's earliest real printing). This file contributes only the DMU-specific
 * presentation row, surfaced via the set's `printings`.
 */
val ShivanReefReprint = Printing(
    oracleId = "0fe16212-66c3-4e45-a641-7391e9b2e304",
    name = "Shivan Reef",
    setCode = "DMU",
    collectorNumber = "255",
    artist = "Andrew Mar",
    imageUri = "https://cards.scryfall.io/normal/front/a/3/a338107b-0960-4496-a9a5-f7b672e7c043.jpg",
    releaseDate = "2022-09-09",
    rarity = Rarity.RARE,
)
