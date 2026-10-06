package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Pillar of Flame reprint in J22. Canonical CardDefinition lives in Avacyn Restored
 * (its earliest real printing), `com.wingedsheep.mtg.sets.definitions.avr.cards.PillarOfFlame`.
 */
val PillarOfFlameReprint = Printing(
    oracleId = "468cfc88-a493-44dc-9d0a-63d9cc89c114",
    name = "Pillar of Flame",
    setCode = "J22",
    collectorNumber = "579",
    scryfallId = "a1cdfbb2-8c85-4302-9114-de25f59af10e",
    artist = "Dave Kendall",
    imageUri = "https://cards.scryfall.io/normal/front/a/1/a1cdfbb2-8c85-4302-9114-de25f59af10e.jpg?1783918921",
    releaseDate = "2022-12-02",
    rarity = Rarity.COMMON,
)
