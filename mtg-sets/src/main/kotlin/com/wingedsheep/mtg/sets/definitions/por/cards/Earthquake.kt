package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Earthquake reprint in POR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes
 * only the POR-specific presentation row.
 */
val EarthquakeReprint = Printing(
    oracleId = "9a40614b-50a3-422c-849e-53c8b7d3d204",
    name = "Earthquake",
    setCode = "POR",
    collectorNumber = "124",
    artist = "Dan Frazier",
    imageUri = "https://cards.scryfall.io/normal/front/2/7/272f65a3-3c0c-417d-b5b6-276a643d643e.jpg",
    releaseDate = "1997-05-01",
    rarity = Rarity.RARE,
)
