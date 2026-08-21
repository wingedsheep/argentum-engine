package com.wingedsheep.mtg.sets.definitions.arc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Gleeful Sabotage reprint in Archenemy. The canonical CardDefinition lives in Shadowmoor
 * (`definitions/shm/cards/GleefulSabotage.kt`); this file contributes only per-printing presentation data.
 */
val GleefulSabotageReprint = Printing(
    oracleId = "25fe48be-95c4-4011-8c60-4628fb5ecfcb",
    name = "Gleeful Sabotage",
    setCode = "ARC",
    collectorNumber = "58",
    scryfallId = "7802c7f8-ca2a-4bac-a579-77a8204aa5d4",
    artist = "Todd Lockwood",
    imageUri = "https://cards.scryfall.io/normal/front/7/8/7802c7f8-ca2a-4bac-a579-77a8204aa5d4.jpg?1783941903",
    releaseDate = "2010-06-18",
    rarity = Rarity.COMMON,
)
