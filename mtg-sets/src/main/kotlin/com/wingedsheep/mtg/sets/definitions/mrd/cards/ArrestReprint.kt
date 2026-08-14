package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Arrest reprint in Mirrodin. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in Mercadian Masques' `cards/` package
 * (earliest real-expansion printing); this file contributes only presentation data.
 */
val ArrestReprint = Printing(
    oracleId = "81728b98-8cf9-4734-a318-69184bb4d15c",
    name = "Arrest",
    setCode = "MRD",
    collectorNumber = "2",
    scryfallId = "a5ca260d-4ed8-4a99-b00a-0be15ba0df9f",
    artist = "Tim Hildebrandt",
    imageUri = "https://cards.scryfall.io/normal/front/a/5/a5ca260d-4ed8-4a99-b00a-0be15ba0df9f.jpg?1783944564",
    releaseDate = "2003-10-02",
    rarity = Rarity.COMMON,
)
