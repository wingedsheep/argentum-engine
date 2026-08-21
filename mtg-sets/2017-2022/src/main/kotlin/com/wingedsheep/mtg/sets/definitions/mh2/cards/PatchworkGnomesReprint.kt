package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Patchwork Gnomes reprint in Modern Horizons 2. The canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in the Tempest (`tmp`) `cards/` package; this file contributes only per-printing
 * presentation data.
 */
val PatchworkGnomesReprint = Printing(
    oracleId = "00ad27a1-9162-408d-ac75-970e45d7e06c",
    name = "Patchwork Gnomes",
    setCode = "MH2",
    collectorNumber = "299",
    scryfallId = "3002ccef-5322-4f99-9fce-3b4303347240",
    artist = "Filip Burburan",
    imageUri = "https://cards.scryfall.io/normal/front/3/0/3002ccef-5322-4f99-9fce-3b4303347240.jpg?1783926776",
    releaseDate = "2021-06-18",
    rarity = Rarity.UNCOMMON,
    borderColor = "black",
)
