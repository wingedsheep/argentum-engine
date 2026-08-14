package com.wingedsheep.mtg.sets.definitions.pc2.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Dimir Aqueduct reprint in Planechase 2012. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Ravnica: City of Guilds (`rav`)
 * `cards/` package (the card's earliest real printing); this file contributes only
 * per-printing presentation data.
 */
val DimirAqueductReprint = Printing(
    oracleId = "378a1d57-e2f1-4b84-9692-1564602e9e99",
    name = "Dimir Aqueduct",
    setCode = "PC2",
    collectorNumber = "116",
    scryfallId = "0685f0aa-625a-4847-af35-8250d803f22b",
    artist = "John Avon",
    imageUri = "https://cards.scryfall.io/normal/front/0/6/0685f0aa-625a-4847-af35-8250d803f22b.jpg?1783940588",
    releaseDate = "2012-06-01",
    rarity = Rarity.COMMON,
)
