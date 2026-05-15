package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Duress reprint in MID.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * USG's `cards/` package (the card's earliest real printing). This file contributes
 * only the MID-specific presentation row.
 */
val DuressReprint = Printing(
    oracleId = "33d405ea-7a9a-4970-b70f-9c05d90dd6f0",
    name = "Duress",
    setCode = "MID",
    collectorNumber = "98",
    scryfallId = "c0a72721-2a4a-4d48-a7b0-2370b90b8619",
    artist = "Paul Scott Canavan",
    imageUri = "https://cards.scryfall.io/normal/front/c/0/c0a72721-2a4a-4d48-a7b0-2370b90b8619.jpg?1634349768",
    releaseDate = "2021-09-24",
    rarity = Rarity.COMMON,
)
