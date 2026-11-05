package com.wingedsheep.mtg.sets.definitions.cmm.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Star of Extinction reprints in CMM — the regular printing (#259) and the etched foil printing
 * (#550). Canonical CardDefinition lives in its earliest set (XLN).
 */
val StarOfExtinctionReprint = Printing(
    oracleId = "48220e6c-5752-46e0-9b7f-f0eef274d929",
    name = "Star of Extinction",
    setCode = "CMM",
    collectorNumber = "259",
    scryfallId = "526d13cb-3616-4ac8-9ac0-04c729d447b2",
    artist = "Chris Rahn",
    imageUri = "https://cards.scryfall.io/normal/front/5/2/526d13cb-3616-4ac8-9ac0-04c729d447b2.jpg?1783915642",
    releaseDate = "2023-08-04",
    rarity = Rarity.RARE,
)

val StarOfExtinctionEtchedReprint = Printing(
    oracleId = "48220e6c-5752-46e0-9b7f-f0eef274d929",
    name = "Star of Extinction",
    setCode = "CMM",
    collectorNumber = "550",
    scryfallId = "8c516be1-ecdf-469f-9d7f-3eeeed962777",
    artist = "Chris Rahn",
    imageUri = "https://cards.scryfall.io/normal/front/8/c/8c516be1-ecdf-469f-9d7f-3eeeed962777.jpg?1783915542",
    releaseDate = "2023-08-04",
    rarity = Rarity.RARE,
)
