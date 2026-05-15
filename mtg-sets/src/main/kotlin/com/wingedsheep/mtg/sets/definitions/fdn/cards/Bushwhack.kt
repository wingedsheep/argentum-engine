package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Bushwhack reprint in FDN.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * BRO's `cards/` package (the card's earliest real printing). This file contributes only
 * the FDN-specific presentation row — set, collector number, art — picked up
 * automatically by `CardDiscovery.findPrintingsIn` and surfaced via the set's
 * `printings`.
 */
val BushwhackReprint = Printing(
    oracleId = "c61374e5-a7f6-455e-a40b-a481751b536b",
    name = "Bushwhack",
    setCode = "FDN",
    collectorNumber = "215",
    scryfallId = "03ebdb36-55e0-49dd-a514-785fbeb4ae19",
    artist = "Artur Nakhodkin",
    imageUri = "https://cards.scryfall.io/normal/front/0/3/03ebdb36-55e0-49dd-a514-785fbeb4ae19.jpg?1730489398",
    releaseDate = "2024-11-15",
    rarity = Rarity.COMMON,
)
