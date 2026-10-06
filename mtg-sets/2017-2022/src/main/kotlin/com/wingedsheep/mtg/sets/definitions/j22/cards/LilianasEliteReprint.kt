package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Liliana's Elite reprint in J22. Canonical CardDefinition lives in Eldritch Moon (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.emn.cards.LilianasElite`.
 */
val LilianasEliteReprint = Printing(
    oracleId = "b6492082-d0ef-4f7c-a4b1-f8ecf6eb0890",
    name = "Liliana's Elite",
    setCode = "J22",
    collectorNumber = "434",
    scryfallId = "5f826404-3df5-4b0c-9739-837938ac8401",
    artist = "Deruchenko Alexander",
    imageUri = "https://cards.scryfall.io/normal/front/5/f/5f826404-3df5-4b0c-9739-837938ac8401.jpg?1783918997",
    releaseDate = "2022-12-02",
    rarity = Rarity.UNCOMMON,
)
