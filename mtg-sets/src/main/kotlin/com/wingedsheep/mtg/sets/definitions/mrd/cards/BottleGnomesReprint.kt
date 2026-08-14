package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Bottle Gnomes reprint in Mirrodin. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in Tempest's `cards/` package (earliest
 * real-expansion printing); this file contributes only presentation data.
 */
val BottleGnomesReprint = Printing(
    oracleId = "54b5e429-7a44-480d-bea4-4f8eeb7449b5",
    name = "Bottle Gnomes",
    setCode = "MRD",
    collectorNumber = "148",
    scryfallId = "018dcb37-221a-4552-9e72-2b9492883eae",
    artist = "Ben Thompson",
    imageUri = "https://cards.scryfall.io/normal/front/0/1/018dcb37-221a-4552-9e72-2b9492883eae.jpg?1783944527",
    releaseDate = "2003-10-02",
    rarity = Rarity.UNCOMMON,
)
