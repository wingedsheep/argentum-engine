package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Blooming Marsh reprint in Outlaws of Thunder Junction. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Kaladesh (`kld`) `cards/` package;
 * this file contributes only per-printing presentation data.
 */
val BloomingMarshReprint = Printing(
    oracleId = "66fa2326-1b5d-41fb-b919-83bf9f383577",
    name = "Blooming Marsh",
    setCode = "OTJ",
    collectorNumber = "266",
    scryfallId = "861caabb-0573-4e94-8b03-342f90465064",
    artist = "Yeong-Hao Han",
    imageUri = "https://cards.scryfall.io/normal/front/8/6/861caabb-0573-4e94-8b03-342f90465064.jpg?1783911773",
    releaseDate = "2024-04-19",
    rarity = Rarity.RARE,
)
