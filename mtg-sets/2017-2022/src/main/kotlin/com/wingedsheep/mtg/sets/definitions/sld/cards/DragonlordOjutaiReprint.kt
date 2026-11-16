package com.wingedsheep.mtg.sets.definitions.sld.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Dragonlord Ojutai reprint in SLD. Canonical CardDefinition lives in its earliest set (DTK).
 * Scryfall lists this printing as a reversible card; both faces are the same card, so only the
 * front face's art is used.
 */
val DragonlordOjutaiReprint = Printing(
    oracleId = "654ff673-4630-4068-9bcf-e88a7877eb58",
    name = "Dragonlord Ojutai",
    setCode = "SLD",
    collectorNumber = "1973",
    scryfallId = "8288607d-57a0-4dce-a333-6ae1c86a6cdf",
    artist = "Crystal Sully",
    imageUri = "https://cards.scryfall.io/normal/front/8/2/8288607d-57a0-4dce-a333-6ae1c86a6cdf.jpg?1783907454",
    releaseDate = "2025-03-24",
    rarity = Rarity.MYTHIC,
)
