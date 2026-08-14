package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Creeping Mold reprint in Mirrodin. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in Visions' `cards/` package (earliest
 * real-expansion printing); this file contributes only presentation data.
 */
val CreepingMoldReprint = Printing(
    oracleId = "59180e94-ccdf-4d9f-9a4a-fe55497d0d63",
    name = "Creeping Mold",
    setCode = "MRD",
    collectorNumber = "117",
    scryfallId = "d6b50bda-f19f-4991-977b-de794f11103d",
    artist = "Dany Orizio",
    imageUri = "https://cards.scryfall.io/normal/front/d/6/d6b50bda-f19f-4991-977b-de794f11103d.jpg?1783944535",
    releaseDate = "2003-10-02",
    rarity = Rarity.UNCOMMON,
)
