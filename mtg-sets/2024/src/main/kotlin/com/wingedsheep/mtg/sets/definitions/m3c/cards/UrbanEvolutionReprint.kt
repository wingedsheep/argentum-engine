package com.wingedsheep.mtg.sets.definitions.m3c.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Urban Evolution reprint in M3C.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in GTC's `cards/` package
 * (the card's earliest real printing). This file contributes only the M3C-specific
 * presentation row — set, collector number, art.
 */
val UrbanEvolutionReprint = Printing(
    oracleId = "c715e525-fc3a-4cab-94a0-04e0a22a5013",
    name = "Urban Evolution",
    setCode = "M3C",
    collectorNumber = "276",
    artist = "Eytan Zana",
    imageUri = "https://cards.scryfall.io/normal/front/6/e/6eecff8f-89c6-425d-baf7-1148b3cfc381.jpg",
    releaseDate = "2024-06-14",
    rarity = Rarity.UNCOMMON,
)
