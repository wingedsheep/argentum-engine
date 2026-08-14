package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Simic Growth Chamber reprint in Commander Legends. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Dissension (`dis`) `cards/` package
 * (the card's earliest real printing); this file contributes only per-printing presentation data.
 */
val SimicGrowthChamberReprint = Printing(
    oracleId = "046f5783-cc7b-416a-8cf6-2bcef9c2cc1a",
    name = "Simic Growth Chamber",
    setCode = "CMR",
    collectorNumber = "492",
    scryfallId = "b0b84ef6-ba13-4c65-987e-cc6aa750086c",
    artist = "John Avon",
    imageUri = "https://cards.scryfall.io/normal/front/b/0/b0b84ef6-ba13-4c65-987e-cc6aa750086c.jpg?1783928679",
    releaseDate = "2020-11-20",
    rarity = Rarity.UNCOMMON,
)
