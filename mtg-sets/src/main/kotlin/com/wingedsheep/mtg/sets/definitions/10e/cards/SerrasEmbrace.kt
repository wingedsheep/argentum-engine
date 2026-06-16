package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Serra's Embrace reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * USG's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SerrasEmbraceReprint = Printing(
    oracleId = "6d6ba936-4a15-4c40-aaa6-71605fb732d1",
    name = "Serra's Embrace",
    setCode = "10E",
    collectorNumber = "40",
    artist = "Zoltan Boros & Gabor Szikszai",
    imageUri = "https://cards.scryfall.io/normal/front/2/b/2ba6c4f3-59cf-4eb8-97fb-c9138005e19c.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)
