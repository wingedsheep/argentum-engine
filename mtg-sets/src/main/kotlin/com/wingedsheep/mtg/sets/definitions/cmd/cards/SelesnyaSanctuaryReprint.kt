package com.wingedsheep.mtg.sets.definitions.cmd.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Selesnya Sanctuary reprint in Commander 2011. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Ravnica: City of Guilds (`rav`)
 * `cards/` package (the card's earliest real printing); this file contributes only
 * per-printing presentation data.
 */
val SelesnyaSanctuaryReprint = Printing(
    oracleId = "00ef1c55-dea1-4564-bd57-66de86cba4df",
    name = "Selesnya Sanctuary",
    setCode = "CMD",
    collectorNumber = "287",
    scryfallId = "90b1a7ea-0c55-4903-8bc2-ab4abcfcd170",
    artist = "John Avon",
    imageUri = "https://cards.scryfall.io/normal/front/9/0/90b1a7ea-0c55-4903-8bc2-ab4abcfcd170.jpg?1783941144",
    releaseDate = "2011-06-17",
    rarity = Rarity.COMMON,
)
