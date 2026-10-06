package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Chandra's Pyrohelix reprint in WAR. Canonical CardDefinition lives in Kaladesh (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.kld.cards.ChandrasPyrohelix`.
 */
val ChandrasPyrohelixReprint = Printing(
    oracleId = "69a37af8-6bcd-42b3-b788-0a357e742cc0",
    name = "Chandra's Pyrohelix",
    setCode = "WAR",
    collectorNumber = "120",
    scryfallId = "1c482f51-9222-4e9e-a9fd-bb14a0afe156",
    artist = "Aleksi Briclot",
    imageUri = "https://cards.scryfall.io/normal/front/1/c/1c482f51-9222-4e9e-a9fd-bb14a0afe156.jpg?1783933431",
    releaseDate = "2019-05-03",
    rarity = Rarity.COMMON,
)
