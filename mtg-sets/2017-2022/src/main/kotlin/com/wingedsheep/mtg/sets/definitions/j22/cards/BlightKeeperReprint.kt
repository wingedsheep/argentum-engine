package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Blight Keeper reprint in J22. Canonical CardDefinition lives in Ixalan (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.xln.cards.BlightKeeper`.
 */
val BlightKeeperReprint = Printing(
    oracleId = "1dc73412-c91c-48c9-bde6-b169add79b37",
    name = "Blight Keeper",
    setCode = "J22",
    collectorNumber = "378",
    scryfallId = "8458f030-f333-486e-8b52-b7422c1ff059",
    artist = "Ben Wootten",
    imageUri = "https://cards.scryfall.io/normal/front/8/4/8458f030-f333-486e-8b52-b7422c1ff059.jpg?1783919023",
    releaseDate = "2022-12-02",
    rarity = Rarity.COMMON,
)
