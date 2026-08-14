package com.wingedsheep.mtg.sets.definitions.cmd.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Izzet Boilerworks reprint in Commander 2011. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Guildpact (`gpt`) `cards/` package
 * (the card's earliest real printing); this file contributes only per-printing presentation data.
 */
val IzzetBoilerworksReprint = Printing(
    oracleId = "1cb9d94a-3039-4f2e-8fcc-6996f9a45f74",
    name = "Izzet Boilerworks",
    setCode = "CMD",
    collectorNumber = "278",
    scryfallId = "7bf5f498-4292-42ff-a10b-251eb86583ac",
    artist = "John Avon",
    imageUri = "https://cards.scryfall.io/normal/front/7/b/7bf5f498-4292-42ff-a10b-251eb86583ac.jpg?1783941147",
    releaseDate = "2011-06-17",
    rarity = Rarity.COMMON,
)
