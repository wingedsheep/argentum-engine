package com.wingedsheep.mtg.sets.definitions.c17.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Boros Garrison reprint in Commander 2017. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Ravnica: City of Guilds (`rav`)
 * `cards/` package (the card's earliest real printing); this file contributes only
 * per-printing presentation data.
 */
val BorosGarrisonReprint = Printing(
    oracleId = "8fa3ac81-3dfe-4565-be99-5554f7597b4b",
    name = "Boros Garrison",
    setCode = "C17",
    collectorNumber = "239",
    scryfallId = "cb8914b9-7339-48dd-9fa1-0a7bccefc872",
    artist = "John Avon",
    imageUri = "https://cards.scryfall.io/normal/front/c/b/cb8914b9-7339-48dd-9fa1-0a7bccefc872.jpg?1783935857",
    releaseDate = "2017-08-25",
    rarity = Rarity.COMMON,
)
