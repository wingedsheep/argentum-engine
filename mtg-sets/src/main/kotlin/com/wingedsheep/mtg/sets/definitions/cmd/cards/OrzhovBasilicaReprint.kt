package com.wingedsheep.mtg.sets.definitions.cmd.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Orzhov Basilica reprint in Commander 2011. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Guildpact (`gpt`) `cards/` package
 * (the card's earliest real printing); this file contributes only per-printing presentation data.
 */
val OrzhovBasilicaReprint = Printing(
    oracleId = "aa00ae0b-7c0f-427e-8102-ce0e2a6af5df",
    name = "Orzhov Basilica",
    setCode = "CMD",
    collectorNumber = "283",
    scryfallId = "dabeb3ae-b7cb-4caa-8f4b-65f8f4912043",
    artist = "John Avon",
    imageUri = "https://cards.scryfall.io/normal/front/d/a/dabeb3ae-b7cb-4caa-8f4b-65f8f4912043.jpg?1783941144",
    releaseDate = "2011-06-17",
    rarity = Rarity.COMMON,
)
