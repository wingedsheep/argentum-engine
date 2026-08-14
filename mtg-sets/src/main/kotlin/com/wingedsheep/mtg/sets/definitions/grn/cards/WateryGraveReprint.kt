package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Watery Grave reprint in Guilds of Ravnica. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Ravnica: City of Guilds (`rav`)
 * `cards/` package (the card's earliest real printing); this file contributes only
 * per-printing presentation data.
 */
val WateryGraveReprint = Printing(
    oracleId = "fc9ec820-4245-4a96-b009-5308a818ca58",
    name = "Watery Grave",
    setCode = "GRN",
    collectorNumber = "259",
    scryfallId = "7d4595f2-9297-40dc-b2dd-7144bbb401f7",
    artist = "Cliff Childs",
    imageUri = "https://cards.scryfall.io/normal/front/7/d/7d4595f2-9297-40dc-b2dd-7144bbb401f7.jpg?1783934098",
    releaseDate = "2018-10-05",
    rarity = Rarity.RARE,
)
