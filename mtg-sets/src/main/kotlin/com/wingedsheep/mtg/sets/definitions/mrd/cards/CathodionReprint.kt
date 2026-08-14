package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Cathodion reprint in Mirrodin. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in Urza's Saga's `cards/` package (earliest
 * real-expansion printing); this file contributes only presentation data.
 */
val CathodionReprint = Printing(
    oracleId = "fcd4f816-2de1-4b30-82fb-cb87f45747ea",
    name = "Cathodion",
    setCode = "MRD",
    collectorNumber = "149",
    scryfallId = "e223e822-a7a0-48d1-9bb7-88ee3d939c6f",
    artist = "Eric Peterson",
    imageUri = "https://cards.scryfall.io/normal/front/e/2/e223e822-a7a0-48d1-9bb7-88ee3d939c6f.jpg?1783944528",
    releaseDate = "2003-10-02",
    rarity = Rarity.UNCOMMON,
)
