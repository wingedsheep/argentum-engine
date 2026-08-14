package com.wingedsheep.mtg.sets.definitions.cmd.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Golgari Rot Farm reprint in Commander 2011. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Ravnica: City of Guilds (`rav`)
 * `cards/` package (the card's earliest real printing); this file contributes only
 * per-printing presentation data.
 */
val GolgariRotFarmReprint = Printing(
    oracleId = "1b301478-b14f-4ef8-94e6-9647d582eabe",
    name = "Golgari Rot Farm",
    setCode = "CMD",
    collectorNumber = "275",
    scryfallId = "43a1fc69-8305-466c-94f3-ec95244cdcb9",
    artist = "John Avon",
    imageUri = "https://cards.scryfall.io/normal/front/4/3/43a1fc69-8305-466c-94f3-ec95244cdcb9.jpg?1783941148",
    releaseDate = "2011-06-17",
    rarity = Rarity.COMMON,
)
