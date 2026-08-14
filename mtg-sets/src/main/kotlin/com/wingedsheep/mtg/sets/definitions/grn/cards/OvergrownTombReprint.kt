package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Overgrown Tomb reprint in Guilds of Ravnica. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Ravnica: City of Guilds (`rav`)
 * `cards/` package (the card's earliest real printing); this file contributes only
 * per-printing presentation data.
 */
val OvergrownTombReprint = Printing(
    oracleId = "975ec9a3-6f20-4177-8211-82526e092538",
    name = "Overgrown Tomb",
    setCode = "GRN",
    collectorNumber = "253",
    scryfallId = "eff1f52c-5c43-4260-aaa0-6920846a191c",
    artist = "Yeong-Hao Han",
    imageUri = "https://cards.scryfall.io/normal/front/e/f/eff1f52c-5c43-4260-aaa0-6920846a191c.jpg?1783934100",
    releaseDate = "2018-10-05",
    rarity = Rarity.RARE,
)
