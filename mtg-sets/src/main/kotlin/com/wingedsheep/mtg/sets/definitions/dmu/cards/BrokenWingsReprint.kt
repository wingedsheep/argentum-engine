package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Broken Wings reprint in DMU. The canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in another set's `cards/` package; this file contributes only presentation data.
 */
val BrokenWingsReprint = Printing(
    oracleId = "5e316864-d55c-496f-8f46-773567896864",
    name = "Broken Wings",
    setCode = "DMU",
    collectorNumber = "157",
    scryfallId = "0ced8d87-ac22-41b0-a632-298159cbb316",
    artist = "Svetlin Velinov",
    imageUri = "https://cards.scryfall.io/normal/front/0/c/0ced8d87-ac22-41b0-a632-298159cbb316.jpg?1782700459",
    releaseDate = "2022-09-09",
    rarity = Rarity.COMMON,
)
