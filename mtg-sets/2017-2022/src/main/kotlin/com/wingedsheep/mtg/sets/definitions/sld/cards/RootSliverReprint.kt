package com.wingedsheep.mtg.sets.definitions.sld.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Root Sliver reprint in SLD. Canonical CardDefinition lives in its earliest set.
 */
val RootSliverReprint = Printing(
    oracleId = "cb752313-dc7f-47fc-9077-9e3298e8f5fb",
    name = "Root Sliver",
    setCode = "SLD",
    collectorNumber = "656",
    scryfallId = "bfb818ad-fba5-4012-a951-f6bcd72d6812",
    artist = "Matt Thompson",
    imageUri = "https://cards.scryfall.io/normal/front/b/f/bfb818ad-fba5-4012-a951-f6bcd72d6812.jpg",
    releaseDate = "2022-12-15",
    rarity = Rarity.RARE,
)
