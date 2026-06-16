package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Skyhunter Patrol reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MRD's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SkyhunterPatrolReprint = Printing(
    oracleId = "aadcff6f-9207-4d90-a12d-4913c96867e2",
    name = "Skyhunter Patrol",
    setCode = "10E",
    collectorNumber = "41",
    artist = "Matt Cavotta",
    imageUri = "https://cards.scryfall.io/normal/front/a/9/a9cda455-34dc-45b6-aafc-825c1c42b67b.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
