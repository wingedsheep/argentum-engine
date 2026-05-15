package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Duress reprint in ONE.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * USG's `cards/` package (the card's earliest real printing). This file contributes
 * only the ONE-specific presentation row.
 */
val DuressReprint = Printing(
    oracleId = "33d405ea-7a9a-4970-b70f-9c05d90dd6f0",
    name = "Duress",
    setCode = "ONE",
    collectorNumber = "92",
    scryfallId = "3557e601-9b71-4ce9-9047-1a8baa72e574",
    artist = "PINDURSKI",
    imageUri = "https://cards.scryfall.io/normal/front/3/5/3557e601-9b71-4ce9-9047-1a8baa72e574.jpg?1675957024",
    releaseDate = "2023-02-10",
    rarity = Rarity.COMMON,
)
