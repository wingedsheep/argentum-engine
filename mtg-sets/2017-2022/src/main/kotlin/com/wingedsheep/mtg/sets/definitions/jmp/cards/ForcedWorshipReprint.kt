package com.wingedsheep.mtg.sets.definitions.jmp.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Forced Worship reprint in JMP. Canonical CardDefinition lives in New Phyrexia (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.nph.cards.ForcedWorship`.
 */
val ForcedWorshipReprint = Printing(
    oracleId = "ea6b1e8e-b6e7-4fce-bfb7-7adf66b9f240",
    name = "Forced Worship",
    setCode = "JMP",
    collectorNumber = "104",
    scryfallId = "aa4004b7-89b6-43f5-8d6e-13db1b08f3b8",
    artist = "Karl Kopinski",
    imageUri = "https://cards.scryfall.io/normal/front/a/a/aa4004b7-89b6-43f5-8d6e-13db1b08f3b8.jpg?1783930472",
    releaseDate = "2020-07-17",
    rarity = Rarity.COMMON,
)
