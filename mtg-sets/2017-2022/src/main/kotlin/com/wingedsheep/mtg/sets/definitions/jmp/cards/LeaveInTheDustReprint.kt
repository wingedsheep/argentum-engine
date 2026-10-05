package com.wingedsheep.mtg.sets.definitions.jmp.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Leave in the Dust reprint in JMP. Canonical CardDefinition lives in Aether Revolt (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.aer.cards.LeaveInTheDust`.
 */
val LeaveInTheDustReprint = Printing(
    oracleId = "7bb843b6-c55b-4796-b1a7-2fff7b05c276",
    name = "Leave in the Dust",
    setCode = "JMP",
    collectorNumber = "156",
    scryfallId = "049955c6-63f5-4f80-8c60-34c890f3c71a",
    artist = "Vincent Proce",
    imageUri = "https://cards.scryfall.io/normal/front/0/4/049955c6-63f5-4f80-8c60-34c890f3c71a.jpg?1783930453",
    releaseDate = "2020-07-17",
    rarity = Rarity.COMMON,
)
