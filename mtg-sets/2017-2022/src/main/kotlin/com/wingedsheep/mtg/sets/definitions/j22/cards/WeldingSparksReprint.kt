package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Welding Sparks reprint in J22. Canonical CardDefinition lives in Kaladesh (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.kld.cards.WeldingSparks`.
 */
val WeldingSparksReprint = Printing(
    oracleId = "82db3a7e-ec3f-4f25-996e-9e1c23c86003",
    name = "Welding Sparks",
    setCode = "J22",
    collectorNumber = "623",
    scryfallId = "2ed0ceb6-f0cf-410d-977d-68889f4b9a4f",
    artist = "Raymond Swanland",
    imageUri = "https://cards.scryfall.io/normal/front/2/e/2ed0ceb6-f0cf-410d-977d-68889f4b9a4f.jpg?1783918898",
    releaseDate = "2022-12-02",
    rarity = Rarity.COMMON,
)
