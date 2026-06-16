package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Ancestor's Chosen reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * JUD's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val AncestorsChosenReprint = Printing(
    oracleId = "fc2ccab7-cab1-4463-b73d-898070136d74",
    name = "Ancestor's Chosen",
    setCode = "10E",
    collectorNumber = "1",
    artist = "Pete Venters",
    imageUri = "https://cards.scryfall.io/normal/front/7/a/7a5cd03c-4227-4551-aa4b-7d119f0468b5.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)
