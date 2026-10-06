package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Primordial Hydra reprint in M13. Canonical CardDefinition lives in Magic 2012 (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.m12.cards.PrimordialHydra`.
 */
val PrimordialHydraReprint = Printing(
    oracleId = "1c36ed3a-c806-47e5-83f9-e44999c67fe5",
    name = "Primordial Hydra",
    setCode = "M13",
    collectorNumber = "183",
    scryfallId = "937deb52-8888-4298-9ae5-0361c6fdbba2",
    artist = "Aleksi Briclot",
    imageUri = "https://cards.scryfall.io/normal/front/9/3/937deb52-8888-4298-9ae5-0361c6fdbba2.jpg?1783940468",
    releaseDate = "2012-07-13",
    rarity = Rarity.MYTHIC,
)
