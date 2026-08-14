package com.wingedsheep.mtg.sets.definitions.c15.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Boros Garrison reprint in Commander 2015. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Ravnica: City of Guilds (`rav`)
 * `cards/` package (the card's earliest real printing); this file contributes only
 * per-printing presentation data.
 */
val BorosGarrisonReprint = Printing(
    oracleId = "8fa3ac81-3dfe-4565-be99-5554f7597b4b",
    name = "Boros Garrison",
    setCode = "C15",
    collectorNumber = "279",
    scryfallId = "607d8f4e-4de8-4b8d-9fe4-862de72218d7",
    artist = "John Avon",
    imageUri = "https://cards.scryfall.io/normal/front/6/0/607d8f4e-4de8-4b8d-9fe4-862de72218d7.jpg?1783938042",
    releaseDate = "2015-11-13",
    rarity = Rarity.COMMON,
)
