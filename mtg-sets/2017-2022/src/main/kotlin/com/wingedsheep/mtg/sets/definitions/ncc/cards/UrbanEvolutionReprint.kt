package com.wingedsheep.mtg.sets.definitions.ncc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Urban Evolution reprint in NCC.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in GTC's `cards/` package
 * (the card's earliest real printing). This file contributes only the NCC-specific
 * presentation row — set, collector number, art.
 */
val UrbanEvolutionReprint = Printing(
    oracleId = "c715e525-fc3a-4cab-94a0-04e0a22a5013",
    name = "Urban Evolution",
    setCode = "NCC",
    collectorNumber = "355",
    scryfallId = "85df7b1b-7cd6-499d-aa9d-5599a79a3512",
    artist = "Eytan Zana",
    imageUri = "https://cards.scryfall.io/normal/front/8/5/85df7b1b-7cd6-499d-aa9d-5599a79a3512.jpg",
    releaseDate = "2022-04-29",
    rarity = Rarity.UNCOMMON,
)
