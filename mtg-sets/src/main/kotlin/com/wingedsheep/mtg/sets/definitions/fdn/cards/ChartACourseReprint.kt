package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Chart a Course reprint in FDN. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in Ixalan's `cards/` package;
 * this file contributes only the FDN presentation row.
 */
val ChartACourseReprint = Printing(
    oracleId = "05878e49-93ad-4144-9c50-a0bb86126c2e",
    name = "Chart a Course",
    setCode = "FDN",
    collectorNumber = "586",
    scryfallId = "7599d459-fe71-48f4-8c65-0f519bb63a68",
    artist = "James Ryman",
    imageUri = "https://cards.scryfall.io/normal/front/7/5/7599d459-fe71-48f4-8c65-0f519bb63a68.jpg?1782688757",
    releaseDate = "2024-11-15",
    rarity = Rarity.UNCOMMON,
)
