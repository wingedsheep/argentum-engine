package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Maniacal Rage reprint in Invasion. The canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in Exodus (`definitions/exo/cards/ManiacalRage.kt`); this file contributes only
 * presentation data for the Invasion printing.
 */
val ManiacalRageReprint = Printing(
    oracleId = "b5dbb212-0399-475b-a831-921f7c76cdc4",
    name = "Maniacal Rage",
    setCode = "INV",
    collectorNumber = "155",
    scryfallId = "3d17886c-fffd-4f0d-b4da-4b5fba18b811",
    artist = "Matt Cavotta",
    imageUri = "https://cards.scryfall.io/normal/front/3/d/3d17886c-fffd-4f0d-b4da-4b5fba18b811.jpg?1595075978",
    releaseDate = "2000-10-02",
    rarity = Rarity.COMMON,
)
