package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Hungry Flames reprint in J22. Canonical CardDefinition lives in Aether Revolt (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.aer.cards.HungryFlames`.
 */
val HungryFlamesReprint = Printing(
    oracleId = "a2c00788-14e7-4ab5-8c71-2a176621a177",
    name = "Hungry Flames",
    setCode = "J22",
    collectorNumber = "553",
    scryfallId = "95e7ec98-fd08-479b-ace6-10c7c700ab51",
    artist = "Izzy",
    imageUri = "https://cards.scryfall.io/normal/front/9/5/95e7ec98-fd08-479b-ace6-10c7c700ab51.jpg?1783918935",
    releaseDate = "2022-12-02",
    rarity = Rarity.COMMON,
)
