package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Duress reprint in FDN.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * USG's `cards/` package (the card's earliest real printing). This file contributes
 * only the FDN-specific presentation row.
 */
val DuressReprint = Printing(
    oracleId = "33d405ea-7a9a-4970-b70f-9c05d90dd6f0",
    name = "Duress",
    setCode = "FDN",
    collectorNumber = "606",
    artist = "PINDURSKI",
    imageUri = "https://cards.scryfall.io/normal/front/3/4/34c3a894-ee75-4db9-a69f-711bb3cc150a.jpg?1730490899",
    releaseDate = "2024-11-15",
    rarity = Rarity.COMMON,
)
