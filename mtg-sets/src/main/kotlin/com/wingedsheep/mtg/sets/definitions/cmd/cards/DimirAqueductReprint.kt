package com.wingedsheep.mtg.sets.definitions.cmd.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Dimir Aqueduct reprint in Commander 2011. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Ravnica: City of Guilds (`rav`)
 * `cards/` package (the card's earliest real printing); this file contributes only
 * per-printing presentation data.
 */
val DimirAqueductReprint = Printing(
    oracleId = "378a1d57-e2f1-4b84-9692-1564602e9e99",
    name = "Dimir Aqueduct",
    setCode = "CMD",
    collectorNumber = "270",
    scryfallId = "bc6ff991-0a6f-4a7d-9281-6c1b80bbaa6b",
    artist = "John Avon",
    imageUri = "https://cards.scryfall.io/normal/front/b/c/bc6ff991-0a6f-4a7d-9281-6c1b80bbaa6b.jpg?1783941150",
    releaseDate = "2011-06-17",
    rarity = Rarity.COMMON,
)
