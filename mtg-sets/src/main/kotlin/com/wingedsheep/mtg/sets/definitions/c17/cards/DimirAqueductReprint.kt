package com.wingedsheep.mtg.sets.definitions.c17.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Dimir Aqueduct reprint in Commander 2017. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Ravnica: City of Guilds (`rav`)
 * `cards/` package (the card's earliest real printing); this file contributes only
 * per-printing presentation data.
 */
val DimirAqueductReprint = Printing(
    oracleId = "378a1d57-e2f1-4b84-9692-1564602e9e99",
    name = "Dimir Aqueduct",
    setCode = "C17",
    collectorNumber = "245",
    scryfallId = "ec5aec45-9b57-45e1-b4b6-d070ff6e6536",
    artist = "John Avon",
    imageUri = "https://cards.scryfall.io/normal/front/e/c/ec5aec45-9b57-45e1-b4b6-d070ff6e6536.jpg?1783935855",
    releaseDate = "2017-08-25",
    rarity = Rarity.UNCOMMON,
)
