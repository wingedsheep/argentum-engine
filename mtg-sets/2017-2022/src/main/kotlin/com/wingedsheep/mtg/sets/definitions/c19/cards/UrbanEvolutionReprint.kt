package com.wingedsheep.mtg.sets.definitions.c19.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Urban Evolution reprint in C19.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in GTC's `cards/` package
 * (the card's earliest real printing). This file contributes only the C19-specific
 * presentation row — set, collector number, art.
 */
val UrbanEvolutionReprint = Printing(
    oracleId = "c715e525-fc3a-4cab-94a0-04e0a22a5013",
    name = "Urban Evolution",
    setCode = "C19",
    collectorNumber = "205",
    scryfallId = "ccad9db2-19e3-4ed9-8dda-9badf510e945",
    artist = "Eytan Zana",
    imageUri = "https://cards.scryfall.io/normal/front/c/c/ccad9db2-19e3-4ed9-8dda-9badf510e945.jpg",
    releaseDate = "2019-08-23",
    rarity = Rarity.UNCOMMON,
)
