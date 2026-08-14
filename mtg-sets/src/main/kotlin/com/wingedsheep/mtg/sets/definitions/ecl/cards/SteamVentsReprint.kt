package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Steam Vents reprint in Lorwyn Eclipsed. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Guildpact (`gpt`) `cards/` package
 * (the card's earliest real printing); this file contributes only per-printing presentation data.
 */
val SteamVentsReprint = Printing(
    oracleId = "17039058-822d-409f-938c-b727a366ba63",
    name = "Steam Vents",
    setCode = "ECL",
    collectorNumber = "267",
    scryfallId = "b66daa94-d367-4812-9f18-f35378c1febb",
    artist = "Adam Paquette",
    imageUri = "https://cards.scryfall.io/normal/front/b/6/b66daa94-d367-4812-9f18-f35378c1febb.jpg?1759144847",
    releaseDate = "2026-01-23",
    rarity = Rarity.RARE,
)
