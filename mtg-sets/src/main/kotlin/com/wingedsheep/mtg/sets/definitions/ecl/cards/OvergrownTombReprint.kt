package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Overgrown Tomb reprint in Lorwyn Eclipsed. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Ravnica: City of Guilds (`rav`)
 * `cards/` package (the card's earliest real printing); this file contributes only
 * per-printing presentation data.
 */
val OvergrownTombReprint = Printing(
    oracleId = "975ec9a3-6f20-4177-8211-82526e092538",
    name = "Overgrown Tomb",
    setCode = "ECL",
    collectorNumber = "266",
    scryfallId = "45b92924-baa1-4c9b-9932-9a5eda8f3446",
    artist = "Adam Paquette",
    imageUri = "https://cards.scryfall.io/normal/front/4/5/45b92924-baa1-4c9b-9932-9a5eda8f3446.jpg?1759144847",
    releaseDate = "2026-01-23",
    rarity = Rarity.RARE,
)
