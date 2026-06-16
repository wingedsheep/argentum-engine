package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Epitaph Golem reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * SOI's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val EpitaphGolemReprint = Printing(
    oracleId = "e3483dd4-0118-4143-9b55-51078a6f274a",
    name = "Epitaph Golem",
    setCode = "INR",
    collectorNumber = "262",
    artist = "Adam Paquette",
    imageUri = "https://cards.scryfall.io/normal/front/2/0/202125f5-9182-436f-86df-701cdc7e60ce.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.COMMON,
)
