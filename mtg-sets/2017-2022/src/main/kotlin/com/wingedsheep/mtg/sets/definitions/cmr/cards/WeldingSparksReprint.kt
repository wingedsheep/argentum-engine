package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Welding Sparks reprint in CMR. Canonical CardDefinition lives in Kaladesh (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.kld.cards.WeldingSparks`.
 */
val WeldingSparksReprint = Printing(
    oracleId = "82db3a7e-ec3f-4f25-996e-9e1c23c86003",
    name = "Welding Sparks",
    setCode = "CMR",
    collectorNumber = "210",
    scryfallId = "346cc190-6825-4226-8ea5-71abe788454d",
    artist = "Raymond Swanland",
    imageUri = "https://cards.scryfall.io/normal/front/3/4/346cc190-6825-4226-8ea5-71abe788454d.jpg?1783928801",
    releaseDate = "2020-11-20",
    rarity = Rarity.COMMON,
)
