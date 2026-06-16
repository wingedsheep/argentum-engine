package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Karplusan Forest reprint in DMU.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in ICE's `cards/` package
 * (the card's earliest real printing). This file contributes only the DMU-specific
 * presentation row, surfaced via the set's `printings`.
 */
val KarplusanForestReprint = Printing(
    oracleId = "bd912666-f37f-4767-af6f-9e6d0fcccacf",
    name = "Karplusan Forest",
    setCode = "DMU",
    collectorNumber = "250",
    artist = "Sam Burley",
    imageUri = "https://cards.scryfall.io/normal/front/b/8/b89b2c79-e3d3-4ef9-bfc8-f9c090975011.jpg",
    releaseDate = "2022-09-09",
    rarity = Rarity.RARE,
)
