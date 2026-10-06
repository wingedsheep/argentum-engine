package com.wingedsheep.mtg.sets.definitions.jmp.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Pillar of Flame reprint in JMP. Canonical CardDefinition lives in Avacyn Restored
 * (its earliest real printing), `com.wingedsheep.mtg.sets.definitions.avr.cards.PillarOfFlame`.
 */
val PillarOfFlameReprint = Printing(
    oracleId = "468cfc88-a493-44dc-9d0a-63d9cc89c114",
    name = "Pillar of Flame",
    setCode = "JMP",
    collectorNumber = "355",
    scryfallId = "ffdb47a8-130b-4ca9-ad29-9484b5c0c582",
    artist = "Dave Kendall",
    imageUri = "https://cards.scryfall.io/normal/front/f/f/ffdb47a8-130b-4ca9-ad29-9484b5c0c582.jpg?1783930379",
    releaseDate = "2020-07-17",
    rarity = Rarity.COMMON,
)
