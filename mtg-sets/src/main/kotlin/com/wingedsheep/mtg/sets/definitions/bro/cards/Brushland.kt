package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Brushland reprint in BRO.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in ICE's `cards/` package
 * (the card's earliest real printing). This file contributes only the BRO-specific
 * presentation row, surfaced via the set's `printings`.
 */
val BrushlandReprint = Printing(
    oracleId = "5eb8b497-ec9a-4a89-ad29-1ec3ca82da7c",
    name = "Brushland",
    setCode = "BRO",
    collectorNumber = "259",
    artist = "Thomas Stoop",
    imageUri = "https://cards.scryfall.io/normal/front/1/8/18d236ce-3b78-403a-b5f9-4fb44123d85b.jpg",
    releaseDate = "2022-11-18",
    rarity = Rarity.RARE,
)
