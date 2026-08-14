package com.wingedsheep.mtg.sets.definitions.khc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Golgari Rot Farm reprint in Kaldheim Commander. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Ravnica: City of Guilds (`rav`)
 * `cards/` package (the card's earliest real printing); this file contributes only
 * per-printing presentation data.
 */
val GolgariRotFarmReprint = Printing(
    oracleId = "1b301478-b14f-4ef8-94e6-9647d582eabe",
    name = "Golgari Rot Farm",
    setCode = "KHC",
    collectorNumber = "112",
    scryfallId = "aee8e11f-6e73-4c8b-bd4e-3956ee18ac66",
    artist = "John Avon",
    imageUri = "https://cards.scryfall.io/normal/front/a/e/aee8e11f-6e73-4c8b-bd4e-3956ee18ac66.jpg?1783928292",
    releaseDate = "2021-02-05",
    rarity = Rarity.UNCOMMON,
)
