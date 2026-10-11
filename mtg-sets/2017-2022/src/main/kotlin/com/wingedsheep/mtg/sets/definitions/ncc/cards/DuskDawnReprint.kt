package com.wingedsheep.mtg.sets.definitions.ncc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Dusk // Dawn reprint in NCC.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in Amonkhet (`akh/cards/DuskDawn.kt`).
 * This file contributes only the NCC-specific presentation row — set, collector number, art.
 */
val DuskDawnReprint = Printing(
    oracleId = "7bc3f92f-68a2-4934-afc4-89f6d0e8cf98",
    name = "Dusk // Dawn",
    setCode = "NCC",
    collectorNumber = "198",
    scryfallId = "db0a5840-cbf7-4899-932b-8bdcbbf2e5f8",
    artist = "Kasia 'Kafis' Zielińska",
    imageUri = "https://cards.scryfall.io/normal/front/d/b/db0a5840-cbf7-4899-932b-8bdcbbf2e5f8.jpg?1783923291",
    releaseDate = "2022-04-29",
    rarity = Rarity.RARE,
)
