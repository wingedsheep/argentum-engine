package com.wingedsheep.mtg.sets.definitions.pz2.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Windgrace's Judgment reprint in Treasure Chest (PZ2).
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in C18's `cards/` package (the
 * card's earliest real printing). This file contributes only the PZ2-specific presentation row.
 */
val WindgracesJudgmentReprint = Printing(
    oracleId = "de1ca6ed-b275-4f62-ba05-f31b3659b352",
    name = "Windgrace's Judgment",
    setCode = "PZ2",
    collectorNumber = "70669",
    scryfallId = "54c97512-ddf2-4c90-a372-aa49aaf96207",
    artist = "Grzegorz Rutkowski",
    imageUri = "https://cards.scryfall.io/normal/front/5/4/54c97512-ddf2-4c90-a372-aa49aaf96207.jpg?1783933948",
    releaseDate = "2018-12-06",
    rarity = Rarity.RARE,
)
