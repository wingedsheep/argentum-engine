package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Steam Vents reprint in Guilds of Ravnica. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Guildpact (`gpt`) `cards/` package
 * (the card's earliest real printing); this file contributes only per-printing presentation data.
 */
val SteamVentsReprint = Printing(
    oracleId = "17039058-822d-409f-938c-b727a366ba63",
    name = "Steam Vents",
    setCode = "GRN",
    collectorNumber = "257",
    scryfallId = "b8ebe3cf-7143-453a-b0ef-2f5bdaac3185",
    artist = "Jonas De Ro",
    imageUri = "https://cards.scryfall.io/normal/front/b/8/b8ebe3cf-7143-453a-b0ef-2f5bdaac3185.jpg?1783934098",
    releaseDate = "2018-10-05",
    rarity = Rarity.RARE,
)
