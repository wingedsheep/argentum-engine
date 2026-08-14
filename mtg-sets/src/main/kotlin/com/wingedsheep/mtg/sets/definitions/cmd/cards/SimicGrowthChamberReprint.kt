package com.wingedsheep.mtg.sets.definitions.cmd.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Simic Growth Chamber reprint in Commander 2011. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Dissension (`dis`) `cards/` package
 * (the card's earliest real printing); this file contributes only per-printing presentation data.
 */
val SimicGrowthChamberReprint = Printing(
    oracleId = "046f5783-cc7b-416a-8cf6-2bcef9c2cc1a",
    name = "Simic Growth Chamber",
    setCode = "CMD",
    collectorNumber = "288",
    scryfallId = "f5d23429-37c3-4990-9f5c-822fb12c76ce",
    artist = "John Avon",
    imageUri = "https://cards.scryfall.io/normal/front/f/5/f5d23429-37c3-4990-9f5c-822fb12c76ce.jpg?1783941142",
    releaseDate = "2011-06-17",
    rarity = Rarity.COMMON,
)
