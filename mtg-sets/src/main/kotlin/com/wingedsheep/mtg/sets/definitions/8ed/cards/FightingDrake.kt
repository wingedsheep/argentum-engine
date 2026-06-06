package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Fighting Drake reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * TMP's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val FightingDrakeReprint = Printing(
    oracleId = "b13cc6be-ee35-491e-a437-f7fc191f24c7",
    name = "Fighting Drake",
    setCode = "8ED",
    collectorNumber = "77",
    artist = "Matt Cavotta",
    imageUri = "https://cards.scryfall.io/normal/front/9/9/99c0a102-b31c-4cbd-894b-44b1c1e677d2.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)
