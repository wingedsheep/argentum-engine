package com.wingedsheep.mtg.sets.definitions.c13.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Orzhov Basilica reprint in Commander 2013. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Guildpact (`gpt`) `cards/` package
 * (the card's earliest real printing); this file contributes only per-printing presentation data.
 */
val OrzhovBasilicaReprint = Printing(
    oracleId = "aa00ae0b-7c0f-427e-8102-ce0e2a6af5df",
    name = "Orzhov Basilica",
    setCode = "C13",
    collectorNumber = "311",
    scryfallId = "c42c3e02-9ef3-4a9d-905c-4385fea401ba",
    artist = "John Avon",
    imageUri = "https://cards.scryfall.io/normal/front/c/4/c42c3e02-9ef3-4a9d-905c-4385fea401ba.jpg?1783939623",
    releaseDate = "2013-11-01",
    rarity = Rarity.COMMON,
)
