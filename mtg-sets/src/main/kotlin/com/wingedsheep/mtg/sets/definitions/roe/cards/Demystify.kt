package com.wingedsheep.mtg.sets.definitions.roe.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Demystify reprint in ROE.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * an earlier set's `cards/` package. This file contributes only the ROE-specific
 * presentation row.
 */
val DemystifyReprint = Printing(
    oracleId = "fd591199-9f7a-4147-a150-13279dbb4498",
    name = "Demystify",
    setCode = "ROE",
    collectorNumber = "18",
    scryfallId = "e109007a-9d4a-4a6d-bf97-edadb9a1c745",
    artist = "Véronique Meignaud",
    imageUri = "https://cards.scryfall.io/normal/front/e/1/e109007a-9d4a-4a6d-bf97-edadb9a1c745.jpg?1562709491",
    releaseDate = "2010-04-23",
    rarity = Rarity.COMMON,
)
