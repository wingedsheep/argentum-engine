package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Sabretooth Tiger reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ICE's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SabretoothTigerReprint = Printing(
    oracleId = "8ea35158-1c1a-46da-b466-0eafb376e464",
    name = "Sabretooth Tiger",
    setCode = "8ED",
    collectorNumber = "217",
    artist = "Monte Michael Moore",
    imageUri = "https://cards.scryfall.io/normal/front/c/8/c85964eb-c586-4e62-9a19-6a665e6ad98d.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
