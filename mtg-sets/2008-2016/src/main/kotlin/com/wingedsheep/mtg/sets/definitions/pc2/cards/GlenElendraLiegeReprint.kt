package com.wingedsheep.mtg.sets.definitions.pc2.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Glen Elendra Liege reprint in Planechase 2012. The canonical CardDefinition lives in Shadowmoor
 * (`definitions/shm/cards/GlenElendraLiege.kt`); this file contributes only per-printing presentation data.
 */
val GlenElendraLiegeReprint = Printing(
    oracleId = "946bba74-0951-408c-b06f-167739b10934",
    name = "Glen Elendra Liege",
    setCode = "PC2",
    collectorNumber = "94",
    scryfallId = "b42f2e85-c321-42f2-b466-2e7af3259a4b",
    artist = "Kev Walker",
    imageUri = "https://cards.scryfall.io/normal/front/b/4/b42f2e85-c321-42f2-b466-2e7af3259a4b.jpg?1783940597",
    releaseDate = "2012-06-01",
    rarity = Rarity.RARE,
)
