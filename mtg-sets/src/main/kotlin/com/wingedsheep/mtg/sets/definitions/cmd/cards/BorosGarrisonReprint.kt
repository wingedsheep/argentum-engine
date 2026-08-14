package com.wingedsheep.mtg.sets.definitions.cmd.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Boros Garrison reprint in Commander 2011. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Ravnica: City of Guilds (`rav`)
 * `cards/` package (the card's earliest real printing); this file contributes only
 * per-printing presentation data.
 */
val BorosGarrisonReprint = Printing(
    oracleId = "8fa3ac81-3dfe-4565-be99-5554f7597b4b",
    name = "Boros Garrison",
    setCode = "CMD",
    collectorNumber = "268",
    scryfallId = "fd0c3f44-b27c-4fc4-9870-21f616259f51",
    artist = "John Avon",
    imageUri = "https://cards.scryfall.io/normal/front/f/d/fd0c3f44-b27c-4fc4-9870-21f616259f51.jpg?1783941150",
    releaseDate = "2011-06-17",
    rarity = Rarity.COMMON,
)
