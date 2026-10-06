package com.wingedsheep.mtg.sets.definitions.jmp.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Sarkhan's Rage reprint in JMP. Canonical CardDefinition lives in Dragons of Tarkir
 * (its earliest real printing), `com.wingedsheep.mtg.sets.definitions.dtk.cards.SarkhansRage`.
 */
val SarkhansRageReprint = Printing(
    oracleId = "3313aa8f-6441-4daa-b8fa-e6144f60bea8",
    name = "Sarkhan's Rage",
    setCode = "JMP",
    collectorNumber = "360",
    scryfallId = "47daba07-1f1e-48e1-a500-ef94d0a3b327",
    artist = "Chris Rahn",
    imageUri = "https://cards.scryfall.io/normal/front/4/7/47daba07-1f1e-48e1-a500-ef94d0a3b327.jpg?1783930378",
    releaseDate = "2020-07-17",
    rarity = Rarity.COMMON,
)
