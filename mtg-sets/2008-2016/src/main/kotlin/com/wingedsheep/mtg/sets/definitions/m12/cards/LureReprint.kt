package com.wingedsheep.mtg.sets.definitions.m12.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Lure reprint in M12.
 * Canonical CardDefinition lives in Limited Edition Alpha (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.lea.cards.Lure`.
 */
val LureM12Reprint = Printing(
    oracleId = "7a7425ba-4478-4bc4-855f-abf947ea4fa2",
    name = "Lure",
    setCode = "M12",
    collectorNumber = "183",
    scryfallId = "c9704ea0-4dad-4b37-a316-d00766e2a723",
    artist = "D. Alexander Gregory",
    imageUri = "https://cards.scryfall.io/normal/front/c/9/c9704ea0-4dad-4b37-a316-d00766e2a723.jpg?1783941057",
    releaseDate = "2011-07-15",
    rarity = Rarity.UNCOMMON,
)
