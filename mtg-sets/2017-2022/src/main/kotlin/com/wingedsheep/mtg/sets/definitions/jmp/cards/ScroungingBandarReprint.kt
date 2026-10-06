package com.wingedsheep.mtg.sets.definitions.jmp.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Scrounging Bandar reprint in JMP. Canonical CardDefinition lives in Aether Revolt (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.aer.cards.ScroungingBandar`.
 */
val ScroungingBandarReprint = Printing(
    oracleId = "8e1eff5a-77e8-4ea2-96b8-547504f83b45",
    name = "Scrounging Bandar",
    setCode = "JMP",
    collectorNumber = "428",
    scryfallId = "3f6f2163-5e08-4465-9669-a5a176a2b810",
    artist = "Shreya Shetty",
    imageUri = "https://cards.scryfall.io/normal/front/3/f/3f6f2163-5e08-4465-9669-a5a176a2b810.jpg?1783930354",
    releaseDate = "2020-07-17",
    rarity = Rarity.COMMON,
)
