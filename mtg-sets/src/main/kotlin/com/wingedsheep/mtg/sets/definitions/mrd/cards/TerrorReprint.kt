package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Terror reprint in MRD.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (the spell script) lives in
 * Limited Edition Alpha's `cards/` package. This file contributes only the MRD-specific
 * presentation row.
 */
val TerrorReprint = Printing(
    oracleId = "b81f041d-98db-4408-9472-c483e4a502bc",
    name = "Terror",
    setCode = "MRD",
    collectorNumber = "79",
    scryfallId = "f41651db-619a-4ab4-86cf-a0d32297dbdf",
    artist = "Puddnhead",
    imageUri = "https://cards.scryfall.io/normal/front/f/4/f41651db-619a-4ab4-86cf-a0d32297dbdf.jpg?1562163040",
    releaseDate = "2003-10-02",
    rarity = Rarity.COMMON,
)
