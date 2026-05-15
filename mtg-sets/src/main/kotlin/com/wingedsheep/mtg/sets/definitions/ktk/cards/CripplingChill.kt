package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Crippling Chill reprint in KTK.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * AVR's `cards/` package (the card's earliest real printing). This file contributes
 * only the KTK-specific presentation row.
 */
val CripplingChillReprint = Printing(
    oracleId = "4459117a-d11c-44f4-9aac-caefc5b2d6f2",
    name = "Crippling Chill",
    setCode = "KTK",
    collectorNumber = "35",
    artist = "Torstein Nordstrand",
    imageUri = "https://cards.scryfall.io/normal/front/a/c/acf53a79-7573-43c2-bd3a-93abea58ba80.jpg?1562791864",
    releaseDate = "2014-09-26",
    rarity = Rarity.COMMON,
)
