package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Merchant of Secrets reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LGN's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val MerchantOfSecretsReprint = Printing(
    oracleId = "f6aebd42-0150-4741-84c2-4c85893640e9",
    name = "Merchant of Secrets",
    setCode = "8ED",
    collectorNumber = "90",
    artist = "Greg Hildebrandt",
    imageUri = "https://cards.scryfall.io/normal/front/8/9/89e40516-99ec-45fb-be51-c8503fa4811e.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
