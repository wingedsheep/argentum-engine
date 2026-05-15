package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Disdainful Stroke reprint in KHM.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * an earlier set's `cards/` package. This file contributes only the KHM-specific
 * presentation row.
 */
val DisdainfulStrokeReprint = Printing(
    oracleId = "11e02134-7b1a-46a4-a89e-7539dd1efada",
    name = "Disdainful Stroke",
    setCode = "KHM",
    collectorNumber = "54",
    scryfallId = "7691ac89-f8ba-493e-aa11-5674a783dffb",
    artist = "Campbell White",
    imageUri = "https://cards.scryfall.io/normal/front/7/6/7691ac89-f8ba-493e-aa11-5674a783dffb.jpg?1631047007",
    releaseDate = "2021-02-05",
    rarity = Rarity.COMMON,
)
