package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Archangel of Tithes reprint in OTJ.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * Magic Origins' (ORI) `cards/` package — its earliest real printing. This file contributes
 * only the OTJ-specific presentation row, picked up automatically by
 * `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ArchangelOfTithesReprint = Printing(
    oracleId = "ff5caed4-0276-476d-8fae-edae2536df7f",
    name = "Archangel of Tithes",
    setCode = "OTJ",
    collectorNumber = "2",
    scryfallId = "c853d04c-864b-491c-8c6f-72d2d4874d2f",
    artist = "Denys Tsiperko",
    imageUri = "https://cards.scryfall.io/normal/front/c/8/c853d04c-864b-491c-8c6f-72d2d4874d2f.jpg?1712355228",
    releaseDate = "2024-04-19",
    rarity = Rarity.MYTHIC,
)
