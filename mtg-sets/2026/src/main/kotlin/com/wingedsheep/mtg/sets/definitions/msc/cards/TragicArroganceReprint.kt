package com.wingedsheep.mtg.sets.definitions.msc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Tragic Arrogance reprints in MSC. Canonical CardDefinition lives in Magic Origins.
 */
val TragicArroganceReprint = Printing(
    oracleId = "8a29bd35-33ef-4317-9fe5-8aaff5d7d64d",
    name = "Tragic Arrogance",
    setCode = "MSC",
    collectorNumber = "144",
    scryfallId = "956d4728-e7a3-43f4-a350-86eca1b95d06",
    artist = "Rimas Valeikis",
    imageUri = "https://cards.scryfall.io/normal/front/9/5/956d4728-e7a3-43f4-a350-86eca1b95d06.jpg?1783903243",
    releaseDate = "2026-06-26",
    rarity = Rarity.RARE,
)

val TragicArroganceReprintB = Printing(
    oracleId = "8a29bd35-33ef-4317-9fe5-8aaff5d7d64d",
    name = "Tragic Arrogance",
    setCode = "MSC",
    collectorNumber = "323",
    scryfallId = "512da48c-a02a-4670-84f3-595b245e0321",
    artist = "Rimas Valeikis",
    imageUri = "https://cards.scryfall.io/normal/front/5/1/512da48c-a02a-4670-84f3-595b245e0321.jpg?1783903173",
    releaseDate = "2026-06-26",
    rarity = Rarity.RARE,
    frameEffects = listOf("extendedart"),
)
