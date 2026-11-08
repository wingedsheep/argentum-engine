package com.wingedsheep.mtg.sets.definitions.cmm.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Inspiring Statuary reprints in CMM — the regular printing (#394) and the etched foil printing
 * (#609). Canonical CardDefinition lives in its earliest set (AER).
 */
val InspiringStatuaryReprint = Printing(
    oracleId = "78b2b68d-6e27-49f3-8373-6e19faf6f679",
    name = "Inspiring Statuary",
    setCode = "CMM",
    collectorNumber = "394",
    scryfallId = "d18446a7-68db-4a0a-9f36-ddf840c3e2c8",
    artist = "Kirsten Zirngibl",
    imageUri = "https://cards.scryfall.io/normal/front/d/1/d18446a7-68db-4a0a-9f36-ddf840c3e2c8.jpg?1783915596",
    releaseDate = "2023-08-04",
    rarity = Rarity.RARE,
)

val InspiringStatuaryEtchedReprint = Printing(
    oracleId = "78b2b68d-6e27-49f3-8373-6e19faf6f679",
    name = "Inspiring Statuary",
    setCode = "CMM",
    collectorNumber = "609",
    scryfallId = "99ae2050-764d-49aa-83db-aca6a46e4e6f",
    artist = "Kirsten Zirngibl",
    imageUri = "https://cards.scryfall.io/normal/front/9/9/99ae2050-764d-49aa-83db-aca6a46e4e6f.jpg?1783915521",
    releaseDate = "2023-08-04",
    rarity = Rarity.RARE,
)
