package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Rowdy Research reprint in Bloomburrow Commander (BLC). The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in Wilds of Eldraine's `cards/` package;
 * this file contributes only the BLC presentation row.
 */
val RowdyResearchReprint = Printing(
    oracleId = "1a33a5c2-5aa1-48e5-95a8-d52cdce980de",
    name = "Rowdy Research",
    setCode = "BLC",
    collectorNumber = "173",
    scryfallId = "22737b82-bb81-4e4d-8ce7-7f06c5692d9b",
    artist = "Bram Sels",
    imageUri = "https://cards.scryfall.io/normal/front/2/2/22737b82-bb81-4e4d-8ce7-7f06c5692d9b.jpg?1783910682",
    releaseDate = "2024-08-02",
    rarity = Rarity.UNCOMMON,
)
