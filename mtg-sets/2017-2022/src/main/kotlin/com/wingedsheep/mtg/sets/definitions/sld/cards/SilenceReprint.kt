package com.wingedsheep.mtg.sets.definitions.sld.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Silence reprint in SLD. Canonical CardDefinition lives in its earliest set.
 */
val SilenceReprint = Printing(
    oracleId = "8aed54cb-d1bb-45ad-adbe-38e55d84ff31",
    name = "Silence",
    setCode = "SLD",
    collectorNumber = "881",
    scryfallId = "71628fb0-9f87-4eb7-b9a2-b6ef85695984",
    artist = "Desmuncubic",
    imageUri = "https://cards.scryfall.io/normal/front/7/1/71628fb0-9f87-4eb7-b9a2-b6ef85695984.jpg",
    releaseDate = "2025-03-24",
    rarity = Rarity.RARE,
)
