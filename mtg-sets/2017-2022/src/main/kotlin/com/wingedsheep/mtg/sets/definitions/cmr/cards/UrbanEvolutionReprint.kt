package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Urban Evolution reprint in CMR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in GTC's `cards/` package
 * (the card's earliest real printing). This file contributes only the CMR-specific
 * presentation row — set, collector number, art.
 */
val UrbanEvolutionReprint = Printing(
    oracleId = "c715e525-fc3a-4cab-94a0-04e0a22a5013",
    name = "Urban Evolution",
    setCode = "CMR",
    collectorNumber = "455",
    scryfallId = "2ba443c5-ed9f-4bc0-8821-0053b536d648",
    artist = "Eytan Zana",
    imageUri = "https://cards.scryfall.io/normal/front/2/b/2ba443c5-ed9f-4bc0-8821-0053b536d648.jpg",
    releaseDate = "2020-11-20",
    rarity = Rarity.UNCOMMON,
)
