package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Eight-and-a-Half-Tails reprint in CLB. The canonical CardDefinition lives in
 * Champions of Kamigawa (`chk`), the card's earliest real printing; this file
 * contributes only per-printing presentation data.
 */
val EightAndAHalfTailsReprint = Printing(
    oracleId = "01c8d29f-4924-4d55-80bc-0c0c06bcb733",
    name = "Eight-and-a-Half-Tails",
    setCode = "CLB",
    collectorNumber = "692",
    scryfallId = "ef722374-0305-4c30-ab45-cb772b252faf",
    artist = "Daren Bader",
    imageUri = "https://cards.scryfall.io/normal/front/e/f/ef722374-0305-4c30-ab45-cb772b252faf.jpg?1783922494",
    releaseDate = "2022-06-10",
    rarity = Rarity.RARE,
    frameEffects = listOf("legendary"),
)
