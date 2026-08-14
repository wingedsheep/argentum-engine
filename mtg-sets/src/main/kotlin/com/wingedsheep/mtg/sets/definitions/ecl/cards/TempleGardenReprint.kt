package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Temple Garden reprint in Lorwyn Eclipsed. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Ravnica: City of Guilds (`rav`)
 * `cards/` package (the card's earliest real printing); this file contributes only
 * per-printing presentation data.
 */
val TempleGardenReprint = Printing(
    oracleId = "f413a83d-a40d-434c-b20a-4c707c0527fa",
    name = "Temple Garden",
    setCode = "ECL",
    collectorNumber = "268",
    scryfallId = "6cdd2a74-63b3-4ff2-9c5a-a85dee63c3c9",
    artist = "Adam Paquette",
    imageUri = "https://cards.scryfall.io/normal/front/6/c/6cdd2a74-63b3-4ff2-9c5a-a85dee63c3c9.jpg?1759144838",
    releaseDate = "2026-01-23",
    rarity = Rarity.RARE,
)
