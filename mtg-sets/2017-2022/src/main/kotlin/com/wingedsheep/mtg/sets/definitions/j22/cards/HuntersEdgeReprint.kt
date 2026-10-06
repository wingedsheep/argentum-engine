package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Hunter's Edge reprint in J22. Canonical CardDefinition lives in Core Set 2021 (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.m21.cards.HuntersEdge`.
 */
val HuntersEdgeReprint = Printing(
    oracleId = "7b428e94-8428-455f-b555-f648fcdd2127",
    name = "Hunter's Edge",
    setCode = "J22",
    collectorNumber = "675",
    scryfallId = "e3ba254e-84bb-4d5c-8372-66f73e556b39",
    artist = "Johann Bodin",
    imageUri = "https://cards.scryfall.io/normal/front/e/3/e3ba254e-84bb-4d5c-8372-66f73e556b39.jpg?1783918866",
    releaseDate = "2022-12-02",
    rarity = Rarity.COMMON,
)
