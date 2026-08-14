package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Odric, Lunarch Marshal reprint in CMR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * SOI's `cards/` package (the card's earliest real printing). This file contributes only
 * the CMR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val OdricLunarchMarshalReprint = Printing(
    oracleId = "bad76170-c773-4be5-9457-20dc9f745cb4",
    name = "Odric, Lunarch Marshal",
    setCode = "CMR",
    collectorNumber = "379",
    artist = "Chase Stone",
    imageUri = "https://cards.scryfall.io/normal/front/1/7/17b429bd-d7da-45f5-988b-7eed0d3efeaa.jpg?1783928729",
    releaseDate = "2020-11-20",
    rarity = Rarity.RARE,
)
