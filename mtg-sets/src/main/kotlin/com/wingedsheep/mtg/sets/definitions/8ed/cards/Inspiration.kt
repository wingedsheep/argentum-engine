package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Inspiration reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * VIS's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val InspirationReprint = Printing(
    oracleId = "8f32ceb2-92c2-4dde-bf73-40bb79c3fcef",
    name = "Inspiration",
    setCode = "8ED",
    collectorNumber = "85",
    artist = "Matt Cavotta",
    imageUri = "https://cards.scryfall.io/normal/front/b/e/be039716-30fc-4f84-8f84-6019065560e4.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
