package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Shimmering Grotto reprint in ISD.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LRW's `cards/` package (the card's earliest real printing). This file contributes only
 * the ISD-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ShimmeringGrottoReprint = Printing(
    oracleId = "bae49475-fe01-400b-8959-f0dde959577c",
    name = "Shimmering Grotto",
    setCode = "ISD",
    collectorNumber = "246",
    artist = "Cliff Childs",
    imageUri = "https://cards.scryfall.io/normal/front/a/4/a48e7a7a-574f-4850-9697-8cb276a5812c.jpg",
    releaseDate = "2011-09-30",
    rarity = Rarity.COMMON,
)
