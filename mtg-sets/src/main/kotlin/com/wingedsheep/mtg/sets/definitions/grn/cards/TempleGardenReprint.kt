package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Temple Garden reprint in Guilds of Ravnica. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Ravnica: City of Guilds (`rav`)
 * `cards/` package (the card's earliest real printing); this file contributes only
 * per-printing presentation data.
 */
val TempleGardenReprint = Printing(
    oracleId = "f413a83d-a40d-434c-b20a-4c707c0527fa",
    name = "Temple Garden",
    setCode = "GRN",
    collectorNumber = "258",
    scryfallId = "2b9b0195-beda-403e-bc27-7ae3be9f318c",
    artist = "Titus Lunter",
    imageUri = "https://cards.scryfall.io/normal/front/2/b/2b9b0195-beda-403e-bc27-7ae3be9f318c.jpg?1783934099",
    releaseDate = "2018-10-05",
    rarity = Rarity.RARE,
)
