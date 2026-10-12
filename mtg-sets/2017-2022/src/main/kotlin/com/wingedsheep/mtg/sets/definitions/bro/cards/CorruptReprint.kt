package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Corrupt reprint in BRO.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in USG's `cards/` package
 * (the card's earliest real printing). This file contributes only the BRO-specific
 * presentation row, surfaced via the set's `printings`.
 */
val CorruptReprint = Printing(
    oracleId = "a88a1c53-7de3-423a-a95a-24ed5666ab96",
    name = "Corrupt",
    setCode = "BRO",
    collectorNumber = "88",
    scryfallId = "c92ea561-80da-4fe1-ad87-ce47d9eb80bc",
    artist = "Julie Dillon",
    imageUri = "https://cards.scryfall.io/normal/front/c/9/c92ea561-80da-4fe1-ad87-ce47d9eb80bc.jpg?1783920094",
    releaseDate = "2022-11-18",
    rarity = Rarity.UNCOMMON,
)
