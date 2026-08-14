package com.wingedsheep.mtg.sets.definitions.arc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Sun Droplet reprint in Archenemy. Canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in Mirrodin (`mrd`) — earliest real printing; this file contributes only
 * presentation data.
 */
val SunDropletReprint = Printing(
    oracleId = "1820af5c-9cc2-4b77-b4ca-86084442f087",
    name = "Sun Droplet",
    setCode = "ARC",
    collectorNumber = "117",
    scryfallId = "2e2cca6f-d615-44c2-872b-7bbef43c6caf",
    artist = "Greg Hildebrandt",
    imageUri = "https://cards.scryfall.io/normal/front/2/e/2e2cca6f-d615-44c2-872b-7bbef43c6caf.jpg?1783941890",
    releaseDate = "2010-06-18",
    rarity = Rarity.UNCOMMON,
)
