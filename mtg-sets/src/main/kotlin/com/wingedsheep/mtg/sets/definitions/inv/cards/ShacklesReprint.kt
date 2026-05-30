package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Shackles reprint in Invasion. The canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in Exodus (the card's earliest real-expansion printing); this file contributes
 * only presentation data for the INV print.
 */
val ShacklesReprint = Printing(
    oracleId = "05861ff2-ec85-43d1-b641-c5d2da819c03",
    name = "Shackles",
    setCode = "INV",
    collectorNumber = "37",
    scryfallId = "35b3da05-9a3e-4827-96b8-5de244128db3",
    artist = "Greg Staples",
    imageUri = "https://cards.scryfall.io/normal/front/3/5/35b3da05-9a3e-4827-96b8-5de244128db3.jpg?1562905863",
    releaseDate = "2000-10-02",
    rarity = Rarity.COMMON,
)
