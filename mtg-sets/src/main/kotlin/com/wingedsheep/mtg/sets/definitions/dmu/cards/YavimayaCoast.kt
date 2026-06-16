package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Yavimaya Coast reprint in DMU.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in APC's `cards/` package
 * (the card's earliest real printing). This file contributes only the DMU-specific
 * presentation row, surfaced via the set's `printings`.
 */
val YavimayaCoastReprint = Printing(
    oracleId = "40b36bc6-c185-4bda-99e7-0118953c2c97",
    name = "Yavimaya Coast",
    setCode = "DMU",
    collectorNumber = "261",
    artist = "Jesper Ejsing",
    imageUri = "https://cards.scryfall.io/normal/front/0/e/0ed6c8b0-f154-4678-89d0-9869864ead8d.jpg",
    releaseDate = "2022-09-09",
    rarity = Rarity.RARE,
)
