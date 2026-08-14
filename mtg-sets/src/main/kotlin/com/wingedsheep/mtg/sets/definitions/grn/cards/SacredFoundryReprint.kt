package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Sacred Foundry reprint in Guilds of Ravnica. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Ravnica: City of Guilds (`rav`)
 * `cards/` package (the card's earliest real printing); this file contributes only
 * per-printing presentation data.
 */
val SacredFoundryReprint = Printing(
    oracleId = "45181cb8-2090-4471-ba90-e5a8f04d525f",
    name = "Sacred Foundry",
    setCode = "GRN",
    collectorNumber = "254",
    scryfallId = "b7b598d0-535d-477d-a33d-d6a10ff5439a",
    artist = "Adam Paquette",
    imageUri = "https://cards.scryfall.io/normal/front/b/7/b7b598d0-535d-477d-a33d-d6a10ff5439a.jpg?1783934100",
    releaseDate = "2018-10-05",
    rarity = Rarity.RARE,
)
