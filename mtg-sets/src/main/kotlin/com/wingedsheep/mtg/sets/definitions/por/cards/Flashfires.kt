package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Flashfires reprint in POR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes
 * only the POR-specific presentation row.
 */
val FlashfiresReprint = Printing(
    oracleId = "c281f436-8c77-48f7-b31c-d40cd7f9ed6a",
    name = "Flashfires",
    setCode = "POR",
    collectorNumber = "129",
    artist = "Dameon Willich",
    imageUri = "https://cards.scryfall.io/normal/front/a/9/a9e88867-6acb-43f8-806b-21480aaa1afc.jpg",
    releaseDate = "1997-05-01",
    rarity = Rarity.UNCOMMON,
)
