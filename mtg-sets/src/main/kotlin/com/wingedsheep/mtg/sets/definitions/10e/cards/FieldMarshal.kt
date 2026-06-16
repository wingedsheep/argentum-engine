package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Field Marshal reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * CSP's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val FieldMarshalReprint = Printing(
    oracleId = "1c0a53fc-8037-46e3-90ea-cb8b73631a83",
    name = "Field Marshal",
    setCode = "10E",
    collectorNumber = "15",
    artist = "Stephen Tappin",
    imageUri = "https://cards.scryfall.io/normal/front/1/e/1e22d287-274e-4222-9ed7-3e609a84ac07.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)
