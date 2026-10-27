package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Port of Karfell reprint in CLB. The canonical CardDefinition lives in Kaldheim (`khm`),
 * the card's earliest real printing; this file contributes only per-printing presentation data.
 */
val PortOfKarfellReprint = Printing(
    oracleId = "f500a8a4-6135-448c-9116-bd2695a72229",
    name = "Port of Karfell",
    setCode = "CLB",
    collectorNumber = "908",
    scryfallId = "62711f58-c52e-4493-b641-a77352bd4ff8",
    artist = "Mauricio Calle",
    imageUri = "https://cards.scryfall.io/normal/front/6/2/62711f58-c52e-4493-b641-a77352bd4ff8.jpg?1783922369",
    releaseDate = "2022-06-10",
    rarity = Rarity.UNCOMMON,
)
