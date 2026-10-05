package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Built to Last reprint in J22. Canonical CardDefinition lives in Kaladesh (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.kld.cards.BuiltToLast`.
 */
val BuiltToLastReprint = Printing(
    oracleId = "403e554b-72a5-48b3-acea-0b4062ffb913",
    name = "Built to Last",
    setCode = "J22",
    collectorNumber = "161",
    scryfallId = "657f3d01-a61a-4702-8331-28f911e8358f",
    artist = "Svetlin Velinov",
    imageUri = "https://cards.scryfall.io/normal/front/6/5/657f3d01-a61a-4702-8331-28f911e8358f.jpg?1783919126",
    releaseDate = "2022-12-02",
    rarity = Rarity.COMMON,
)
