package com.wingedsheep.mtg.sets.definitions.cmd.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Windfall reprint in CMD. Canonical CardDefinition lives in its earliest set.
 */
val WindfallReprint = Printing(
    oracleId = "08becc07-28bc-4a2f-a6b0-28a2998d2f50",
    name = "Windfall",
    setCode = "CMD",
    collectorNumber = "70",
    scryfallId = "8d815e5f-ea9b-4926-b38a-ad2112fef1dd",
    artist = "Pete Venters",
    imageUri = "https://cards.scryfall.io/normal/front/8/d/8d815e5f-ea9b-4926-b38a-ad2112fef1dd.jpg",
    releaseDate = "2011-06-17",
    rarity = Rarity.UNCOMMON,
)
