package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Reminisce reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ONS's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ReminisceReprint = Printing(
    oracleId = "296c4d2b-e41c-416b-807e-5b4db638ec57",
    name = "Reminisce",
    setCode = "10E",
    collectorNumber = "99",
    artist = "Ralph Horsley",
    imageUri = "https://cards.scryfall.io/normal/front/f/4/f49a8b0d-f130-4a16-a79c-607618cc40bd.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)
