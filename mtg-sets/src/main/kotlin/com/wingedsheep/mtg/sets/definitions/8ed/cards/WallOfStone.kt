package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Wall of Stone reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val WallOfStoneReprint = Printing(
    oracleId = "cd4cadb4-3156-49bd-b36e-12ba5c85938b",
    name = "Wall of Stone",
    setCode = "8ED",
    collectorNumber = "232",
    artist = "Rob Alexander",
    imageUri = "https://cards.scryfall.io/normal/front/7/1/718da336-ad97-41a6-86bd-4f124e2cc716.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)
