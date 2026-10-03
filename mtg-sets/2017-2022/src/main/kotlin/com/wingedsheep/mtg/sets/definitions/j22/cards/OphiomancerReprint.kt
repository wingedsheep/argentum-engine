package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Ophiomancer reprint in J22. Canonical CardDefinition lives in Commander 2013 (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.c13.cards.Ophiomancer`.
 */
val OphiomancerReprint = Printing(
    oracleId = "55eca80c-dcd8-4c2f-aa0f-fb0aec7b80f7",
    name = "Ophiomancer",
    setCode = "J22",
    collectorNumber = "452",
    scryfallId = "6c44fca9-3a41-41c3-aa74-f0f5a81e8b7d",
    artist = "John Stanko",
    imageUri = "https://cards.scryfall.io/normal/front/6/c/6c44fca9-3a41-41c3-aa74-f0f5a81e8b7d.jpg?1783918987",
    releaseDate = "2022-12-02",
    rarity = Rarity.RARE,
)
