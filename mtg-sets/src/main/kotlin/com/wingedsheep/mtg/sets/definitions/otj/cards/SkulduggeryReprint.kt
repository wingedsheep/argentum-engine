package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Skulduggery reprint in Outlaws of Thunder Junction.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (spell script) lives in Ixalan's
 * `cards/` package. This file contributes only the OTJ-specific presentation row — set,
 * collector number, art — picked up automatically by `CardDiscovery.findPrintingsIn`.
 */
val SkulduggeryReprint = Printing(
    oracleId = "9100530e-2bd0-487e-a4eb-13dabd55b678",
    name = "Skulduggery",
    setCode = "OTJ",
    collectorNumber = "107",
    scryfallId = "03709166-164a-4075-ad0d-ea3b516ab771",
    artist = "Miro Petrov",
    imageUri = "https://cards.scryfall.io/normal/front/0/3/03709166-164a-4075-ad0d-ea3b516ab771.jpg?1712355676",
    releaseDate = "2024-04-19",
    rarity = Rarity.COMMON,
)
