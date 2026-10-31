package com.wingedsheep.mtg.sets.definitions.ncc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Primal Empathy reprint in New Capenna Commander. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in Ikoria's `cards/` package; this file
 * contributes only the New Capenna Commander presentation row.
 */
val PrimalEmpathyReprint = Printing(
    oracleId = "24645d9e-5e6d-411d-be41-c2bd1659937f",
    name = "Primal Empathy",
    setCode = "NCC",
    collectorNumber = "348",
    scryfallId = "b8099a93-c59d-40a9-96e2-f3dd2a6dd8b8",
    artist = "Micah Epstein",
    imageUri = "https://cards.scryfall.io/normal/front/b/8/b8099a93-c59d-40a9-96e2-f3dd2a6dd8b8.jpg?1783923225",
    releaseDate = "2022-04-29",
    rarity = Rarity.UNCOMMON,
)
