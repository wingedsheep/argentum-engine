package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Tempest of Light reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MRD's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val TempestOfLightReprint = Printing(
    oracleId = "9399ed27-31c7-4d6a-99eb-644b74e67344",
    name = "Tempest of Light",
    setCode = "10E",
    collectorNumber = "51",
    artist = "Wayne England",
    imageUri = "https://cards.scryfall.io/normal/front/1/4/1475f999-5a85-4177-aa30-15c51cf69812.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)
