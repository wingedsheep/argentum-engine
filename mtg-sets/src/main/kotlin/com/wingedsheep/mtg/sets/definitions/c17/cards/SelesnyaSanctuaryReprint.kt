package com.wingedsheep.mtg.sets.definitions.c17.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Selesnya Sanctuary reprint in Commander 2017. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Ravnica: City of Guilds (`rav`)
 * `cards/` package (the card's earliest real printing); this file contributes only
 * per-printing presentation data.
 */
val SelesnyaSanctuaryReprint = Printing(
    oracleId = "00ef1c55-dea1-4564-bd57-66de86cba4df",
    name = "Selesnya Sanctuary",
    setCode = "C17",
    collectorNumber = "280",
    scryfallId = "7783b333-4039-45e5-958d-478d86170e4e",
    artist = "John Avon",
    imageUri = "https://cards.scryfall.io/normal/front/7/7/7783b333-4039-45e5-958d-478d86170e4e.jpg?1783935842",
    releaseDate = "2017-08-25",
    rarity = Rarity.COMMON,
)
