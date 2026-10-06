package com.wingedsheep.mtg.sets.definitions.pc2.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Tormented Soul reprint in PC2. Canonical CardDefinition lives in Magic 2012 (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.m12.cards.TormentedSoul`.
 */
val TormentedSoulReprint = Printing(
    oracleId = "ee334bd7-d344-4684-85c8-d0464bbaf2b4",
    name = "Tormented Soul",
    setCode = "PC2",
    collectorNumber = "38",
    scryfallId = "5241b906-7d39-4110-89f2-a04f0163f1a6",
    artist = "Karl Kopinski",
    imageUri = "https://cards.scryfall.io/normal/front/5/2/5241b906-7d39-4110-89f2-a04f0163f1a6.jpg?1783940624",
    releaseDate = "2012-06-01",
    rarity = Rarity.COMMON,
)
