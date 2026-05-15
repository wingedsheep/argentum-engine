package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Arc Lightning reprint in KTK.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * USG's `cards/` package (the card's earliest real printing). This file contributes
 * only the KTK-specific presentation row — set, collector number, art — picked up
 * automatically by `CardDiscovery.findPrintingsIn` and surfaced via the set's
 * `printings`.
 */
val ArcLightningReprint = Printing(
    oracleId = "5acc8b39-3c3e-4012-8cfd-ac3c2c4ca982",
    name = "Arc Lightning",
    setCode = "KTK",
    collectorNumber = "97",
    scryfallId = "35c7c392-6782-40c8-bb24-6aad24f14660",
    artist = "Seb McKinnon",
    imageUri = "https://cards.scryfall.io/normal/front/3/5/35c7c392-6782-40c8-bb24-6aad24f14660.jpg?1562784760",
    releaseDate = "2014-09-26",
    rarity = Rarity.UNCOMMON,
)
