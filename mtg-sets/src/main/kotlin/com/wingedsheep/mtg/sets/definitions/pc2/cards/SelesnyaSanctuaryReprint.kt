package com.wingedsheep.mtg.sets.definitions.pc2.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Selesnya Sanctuary reprint in Planechase 2012. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Ravnica: City of Guilds (`rav`)
 * `cards/` package (the card's earliest real printing); this file contributes only
 * per-printing presentation data.
 */
val SelesnyaSanctuaryReprint = Printing(
    oracleId = "00ef1c55-dea1-4564-bd57-66de86cba4df",
    name = "Selesnya Sanctuary",
    setCode = "PC2",
    collectorNumber = "125",
    scryfallId = "6f2dc0ff-d4a5-4f77-8ea5-bbe35faa99a4",
    artist = "John Avon",
    imageUri = "https://cards.scryfall.io/normal/front/6/f/6f2dc0ff-d4a5-4f77-8ea5-bbe35faa99a4.jpg?1783940583",
    releaseDate = "2012-06-01",
    rarity = Rarity.COMMON,
)
