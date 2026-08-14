package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Verdant Catacombs extended-art reprint in Modern Horizons 2. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Zendikar (`zen`) `cards/` package; this
 * file contributes only per-printing presentation data.
 */
val VerdantCatacombsExtendedArtReprint = Printing(
    oracleId = "67d60b24-d429-4ded-90d9-06e49f28c396",
    name = "Verdant Catacombs",
    setCode = "MH2",
    collectorNumber = "479",
    scryfallId = "c7057b04-e22f-4e33-9f08-b9fbd2e54bf5",
    artist = "Vance Kovacs",
    imageUri = "https://cards.scryfall.io/normal/front/c/7/c7057b04-e22f-4e33-9f08-b9fbd2e54bf5.jpg?1783926701",
    releaseDate = "2021-06-18",
    rarity = Rarity.RARE,
    frameEffects = listOf("extendedart"),
    borderColor = "black",
)
