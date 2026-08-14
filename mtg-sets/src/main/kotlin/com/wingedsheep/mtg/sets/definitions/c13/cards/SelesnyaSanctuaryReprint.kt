package com.wingedsheep.mtg.sets.definitions.c13.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Selesnya Sanctuary reprint in Commander 2013. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Ravnica: City of Guilds (`rav`)
 * `cards/` package (the card's earliest real printing); this file contributes only
 * per-printing presentation data.
 */
val SelesnyaSanctuaryReprint = Printing(
    oracleId = "00ef1c55-dea1-4564-bd57-66de86cba4df",
    name = "Selesnya Sanctuary",
    setCode = "C13",
    collectorNumber = "322",
    scryfallId = "3e1f5d20-ae92-4a4b-a892-23f2cc2836a4",
    artist = "John Avon",
    imageUri = "https://cards.scryfall.io/normal/front/3/e/3e1f5d20-ae92-4a4b-a892-23f2cc2836a4.jpg?1783939621",
    releaseDate = "2013-11-01",
    rarity = Rarity.COMMON,
)
