package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Annul reprint in KHM.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * USG's `cards/` package (the card's earliest real printing). This file contributes
 * only the KHM-specific presentation row.
 */
val AnnulReprint = Printing(
    oracleId = "d08e9784-75f7-4164-ac48-d06160f8c56b",
    name = "Annul",
    setCode = "KHM",
    collectorNumber = "42",
    scryfallId = "4b1d4a59-11a0-4a55-8ac0-07377a9e6dc8",
    artist = "Caio Monteiro",
    imageUri = "https://cards.scryfall.io/normal/front/4/b/4b1d4a59-11a0-4a55-8ac0-07377a9e6dc8.jpg?1631046631",
    releaseDate = "2021-02-05",
    rarity = Rarity.COMMON,
)
