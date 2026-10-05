package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Defy Death reprint in J22. Canonical CardDefinition lives in Avacyn Restored (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.avr.cards.DefyDeath`.
 */
val DefyDeathReprint = Printing(
    oracleId = "dbf9c3e8-35dd-4b80-bd23-f43e21cfd991",
    name = "Defy Death",
    setCode = "J22",
    collectorNumber = "174",
    scryfallId = "500edf50-7d23-453d-baf9-e755939cabdc",
    artist = "Karl Kopinski",
    imageUri = "https://cards.scryfall.io/normal/front/5/0/500edf50-7d23-453d-baf9-e755939cabdc.jpg?1783919119",
    releaseDate = "2022-12-02",
    rarity = Rarity.UNCOMMON,
)
