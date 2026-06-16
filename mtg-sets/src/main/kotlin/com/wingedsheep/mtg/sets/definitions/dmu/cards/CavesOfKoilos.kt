package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Caves of Koilos reprint in DMU.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in APC's `cards/` package
 * (the card's earliest real printing). This file contributes only the DMU-specific
 * presentation row, surfaced via the set's `printings`.
 */
val CavesOfKoilosReprint = Printing(
    oracleId = "33de01e9-ce5a-42d4-afcb-343cd54a6d80",
    name = "Caves of Koilos",
    setCode = "DMU",
    collectorNumber = "244",
    artist = "Julian Kok Joon Wen",
    imageUri = "https://cards.scryfall.io/normal/front/9/9/99264beb-2149-4cd4-9880-f0dc5c570c1b.jpg",
    releaseDate = "2022-09-09",
    rarity = Rarity.RARE,
)
