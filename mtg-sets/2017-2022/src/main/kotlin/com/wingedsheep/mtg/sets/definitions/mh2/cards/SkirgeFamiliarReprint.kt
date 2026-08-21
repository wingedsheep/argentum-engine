package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Skirge Familiar reprint in Modern Horizons 2. The canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in the Urza's Saga (`usg`) `cards/` package; this file contributes only per-printing
 * presentation data.
 */
val SkirgeFamiliarReprint = Printing(
    oracleId = "ba95f24d-42da-48ce-bcf1-1b7c4b3c45b5",
    name = "Skirge Familiar",
    setCode = "MH2",
    collectorNumber = "276",
    scryfallId = "2d92fcf1-2ccd-47d2-9a24-f44b766a0b68",
    artist = "Uriah Voth",
    imageUri = "https://cards.scryfall.io/normal/front/2/d/2d92fcf1-2ccd-47d2-9a24-f44b766a0b68.jpg?1783926785",
    releaseDate = "2021-06-18",
    rarity = Rarity.UNCOMMON,
    borderColor = "black",
)
