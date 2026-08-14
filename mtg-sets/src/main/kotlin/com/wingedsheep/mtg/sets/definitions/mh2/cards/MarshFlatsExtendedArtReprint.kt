package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Marsh Flats extended-art reprint in Modern Horizons 2. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Zendikar (`zen`) `cards/` package; this
 * file contributes only per-printing presentation data.
 */
val MarshFlatsExtendedArtReprint = Printing(
    oracleId = "dab520d0-20b4-4273-ba6b-eb07f85ea433",
    name = "Marsh Flats",
    setCode = "MH2",
    collectorNumber = "476",
    scryfallId = "4e8397e6-d13a-4996-8fde-b8a9895de287",
    artist = "Izzy",
    imageUri = "https://cards.scryfall.io/normal/front/4/e/4e8397e6-d13a-4996-8fde-b8a9895de287.jpg?1783926703",
    releaseDate = "2021-06-18",
    rarity = Rarity.RARE,
    frameEffects = listOf("extendedart"),
    borderColor = "black",
)
