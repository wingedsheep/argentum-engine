package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Temporal Adept reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * UDS's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val TemporalAdeptReprint = Printing(
    oracleId = "fd9b5462-6578-4bf6-8026-23eaf5af3eee",
    name = "Temporal Adept",
    setCode = "8ED",
    collectorNumber = "106",
    artist = "Roger Raupp",
    imageUri = "https://cards.scryfall.io/normal/front/0/7/078e44c6-90bb-4294-9e0a-6194764af00c.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.RARE,
)
