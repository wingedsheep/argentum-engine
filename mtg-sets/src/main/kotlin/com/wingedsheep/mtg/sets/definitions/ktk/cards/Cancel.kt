package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Cancel reprint in KTK.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in TSP's `cards/` package
 * (the card's earliest real printing). This file contributes only the KTK-specific
 * presentation row, surfaced via the set's `printings`.
 */
val CancelReprint = Printing(
    oracleId = "7d00fb28-ea6c-49a9-b4af-ffb38860a9a7",
    name = "Cancel",
    setCode = "KTK",
    collectorNumber = "33",
    artist = "Slawomir Maniak",
    imageUri = "https://cards.scryfall.io/normal/front/9/f/9f540dcb-8d0b-4d33-8c0d-893fa5db54eb.jpg",
    releaseDate = "2014-09-26",
    rarity = Rarity.COMMON,
)
