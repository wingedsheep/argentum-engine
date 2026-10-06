package com.wingedsheep.mtg.sets.definitions.jmp.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Hungry Flames reprint in JMP. Canonical CardDefinition lives in Aether Revolt (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.aer.cards.HungryFlames`.
 */
val HungryFlamesReprint = Printing(
    oracleId = "a2c00788-14e7-4ab5-8c71-2a176621a177",
    name = "Hungry Flames",
    setCode = "JMP",
    collectorNumber = "336",
    scryfallId = "07392a36-e63a-4648-b8df-1172403922eb",
    artist = "Izzy",
    imageUri = "https://cards.scryfall.io/normal/front/0/7/07392a36-e63a-4648-b8df-1172403922eb.jpg?1783930387",
    releaseDate = "2020-07-17",
    rarity = Rarity.COMMON,
)
