package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Stream of Life reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val StreamOfLifeReprint = Printing(
    oracleId = "9eb2912d-2130-49f2-9529-b58fa5a97a15",
    name = "Stream of Life",
    setCode = "8ED",
    collectorNumber = "282",
    artist = "Andrew Goldhawk",
    imageUri = "https://cards.scryfall.io/normal/front/0/4/04ff34fb-7bc5-4484-ad73-067363a2cc16.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)
