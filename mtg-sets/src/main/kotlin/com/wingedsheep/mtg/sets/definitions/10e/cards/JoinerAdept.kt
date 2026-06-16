package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Joiner Adept reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * 5DN's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val JoinerAdeptReprint = Printing(
    oracleId = "139b098c-d4be-4dca-9bde-9eb8ea97c7bf",
    name = "Joiner Adept",
    setCode = "10E",
    collectorNumber = "271",
    artist = "Heather Hudson",
    imageUri = "https://cards.scryfall.io/normal/front/6/1/61751365-3acf-4eb8-b1f9-358ebccccf94.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)
