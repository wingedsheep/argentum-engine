package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Dusk // Dawn reprint in BLC.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in Amonkhet (`akh/cards/DuskDawn.kt`).
 * This file contributes only the BLC-specific presentation row — set, collector number, art.
 */
val DuskDawnReprint = Printing(
    oracleId = "7bc3f92f-68a2-4934-afc4-89f6d0e8cf98",
    name = "Dusk // Dawn",
    setCode = "BLC",
    collectorNumber = "138",
    scryfallId = "00bbcc83-7307-46cc-b164-e1ff502d18a3",
    artist = "Kasia 'Kafis' Zielińska",
    imageUri = "https://cards.scryfall.io/normal/front/0/0/00bbcc83-7307-46cc-b164-e1ff502d18a3.jpg?1783910693",
    releaseDate = "2024-08-02",
    rarity = Rarity.RARE,
)
