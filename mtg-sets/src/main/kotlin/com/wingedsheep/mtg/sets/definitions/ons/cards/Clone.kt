package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Clone reprint in ONS.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes
 * only the ONS-specific presentation row.
 */
val CloneReprint = Printing(
    oracleId = "42226b87-0746-4ebf-9fd0-108d508462af",
    name = "Clone",
    setCode = "ONS",
    collectorNumber = "75",
    artist = "Carl Critchlow",
    imageUri = "https://cards.scryfall.io/normal/front/1/d/1d513dde-7c5f-46f1-b871-5290595bdbbe.jpg",
    releaseDate = "2002-10-07",
    rarity = Rarity.RARE,
)
