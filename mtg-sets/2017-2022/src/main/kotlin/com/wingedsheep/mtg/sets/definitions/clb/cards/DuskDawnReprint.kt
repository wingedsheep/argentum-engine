package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Dusk // Dawn reprint in CLB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in Amonkhet (`akh/cards/DuskDawn.kt`).
 * This file contributes only the CLB-specific presentation row — set, collector number, art.
 */
val DuskDawnReprint = Printing(
    oracleId = "7bc3f92f-68a2-4934-afc4-89f6d0e8cf98",
    name = "Dusk // Dawn",
    setCode = "CLB",
    collectorNumber = "691",
    scryfallId = "f295b713-1d6a-43fd-910d-fb35414bf58a",
    artist = "Kasia 'Kafis' Zielińska",
    imageUri = "https://cards.scryfall.io/normal/front/f/2/f295b713-1d6a-43fd-910d-fb35414bf58a.jpg?1783922495",
    releaseDate = "2022-06-10",
    rarity = Rarity.RARE,
)
