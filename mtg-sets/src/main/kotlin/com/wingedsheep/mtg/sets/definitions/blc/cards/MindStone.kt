package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Mind Stone reprint in BLC.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in WTH's `cards/` package
 * (the card's earliest real printing). This file contributes only the BLC-specific
 * presentation row, surfaced via the set's `printings`.
 */
val MindStoneReprint = Printing(
    oracleId = "c97361b5-af16-4a7b-af85-a429dbaf4ad2",
    name = "Mind Stone",
    setCode = "BLC",
    collectorNumber = "280",
    artist = "Adam Rex",
    imageUri = "https://cards.scryfall.io/normal/front/5/c/5cf95476-27ed-487d-8459-c97a921bb808.jpg",
    releaseDate = "2024-08-02",
    rarity = Rarity.UNCOMMON,
)
