package com.wingedsheep.mtg.sets.definitions.c13.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Boros Garrison reprint in Commander 2013. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Ravnica: City of Guilds (`rav`)
 * `cards/` package (the card's earliest real printing); this file contributes only
 * per-printing presentation data.
 */
val BorosGarrisonReprint = Printing(
    oracleId = "8fa3ac81-3dfe-4565-be99-5554f7597b4b",
    name = "Boros Garrison",
    setCode = "C13",
    collectorNumber = "279",
    scryfallId = "c468dd1c-6f0a-4679-9d33-17e17db8841d",
    artist = "John Avon",
    imageUri = "https://cards.scryfall.io/normal/front/c/4/c468dd1c-6f0a-4679-9d33-17e17db8841d.jpg?1783939630",
    releaseDate = "2013-11-01",
    rarity = Rarity.COMMON,
)
