package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Izzet Boilerworks reprint in Commander Legends: Battle for Baldur's Gate. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Guildpact (`gpt`) `cards/` package
 * (the card's earliest real printing); this file contributes only per-printing presentation data.
 */
val IzzetBoilerworksReprint = Printing(
    oracleId = "1cb9d94a-3039-4f2e-8fcc-6996f9a45f74",
    name = "Izzet Boilerworks",
    setCode = "CLB",
    collectorNumber = "897",
    scryfallId = "c86e42c6-342b-443f-9b99-a68cf536ff45",
    artist = "John Avon",
    imageUri = "https://cards.scryfall.io/normal/front/c/8/c86e42c6-342b-443f-9b99-a68cf536ff45.jpg?1783922374",
    releaseDate = "2022-06-10",
    rarity = Rarity.UNCOMMON,
)
