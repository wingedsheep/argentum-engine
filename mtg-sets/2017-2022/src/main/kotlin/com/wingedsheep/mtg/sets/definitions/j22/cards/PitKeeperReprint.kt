package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Pit Keeper reprint in J22. Canonical CardDefinition lives in Time Spiral (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.tsp.cards.PitKeeper`.
 */
val PitKeeperReprint = Printing(
    oracleId = "e1c5bfae-07cb-4194-8a86-201503e17085",
    name = "Pit Keeper",
    setCode = "J22",
    collectorNumber = "455",
    scryfallId = "0ef90fdf-a88b-4060-85cf-e0180f685bd1",
    artist = "Anthony S. Waters",
    imageUri = "https://cards.scryfall.io/normal/front/0/e/0ef90fdf-a88b-4060-85cf-e0180f685bd1.jpg?1783918985",
    releaseDate = "2022-12-02",
    rarity = Rarity.COMMON,
)
