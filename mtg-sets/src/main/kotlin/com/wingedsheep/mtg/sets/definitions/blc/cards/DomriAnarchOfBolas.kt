package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Domri, Anarch of Bolas reprint in BLC.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * an earlier set's `cards/` package. This file contributes only the BLC-specific
 * presentation row.
 */
val DomriAnarchOfBolasReprint = Printing(
    oracleId = "afc2269c-d3b5-487d-9445-800c7a8e526b",
    name = "Domri, Anarch of Bolas",
    setCode = "BLC",
    collectorNumber = "98",
    scryfallId = "c3aa2926-050c-460d-b26d-e44a39258dcc",
    artist = "Edgar Sánchez Hidalgo",
    imageUri = "https://cards.scryfall.io/normal/front/c/3/c3aa2926-050c-460d-b26d-e44a39258dcc.jpg?1722384711",
    releaseDate = "2024-08-02",
    rarity = Rarity.RARE,
)
