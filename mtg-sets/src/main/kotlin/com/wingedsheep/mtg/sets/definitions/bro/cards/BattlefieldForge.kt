package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Battlefield Forge reprint in BRO.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in APC's `cards/` package
 * (the card's earliest real printing). This file contributes only the BRO-specific
 * presentation row, surfaced via the set's `printings`.
 */
val BattlefieldForgeReprint = Printing(
    oracleId = "6b75b94e-83b7-457e-ac41-7ca90b5a59aa",
    name = "Battlefield Forge",
    setCode = "BRO",
    collectorNumber = "257",
    artist = "Thomas Stoop",
    imageUri = "https://cards.scryfall.io/normal/front/6/4/642584bb-7586-4796-9b94-f01ec5bd9e9f.jpg",
    releaseDate = "2022-11-18",
    rarity = Rarity.RARE,
)
