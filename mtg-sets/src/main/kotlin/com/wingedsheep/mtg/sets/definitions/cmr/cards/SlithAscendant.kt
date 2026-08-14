package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Slith Ascendant reprint in CMR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MRD's `cards/` package (the card's earliest real printing). This file contributes only
 * the CMR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 *
 * Note the rarity drop: uncommon in Mirrodin, common in Commander Legends.
 */
val SlithAscendantReprint = Printing(
    oracleId = "72f5095f-b4ba-45ba-82e5-9a22bddd544b",
    name = "Slith Ascendant",
    setCode = "CMR",
    collectorNumber = "49",
    artist = "Justin Sweet",
    imageUri = "https://cards.scryfall.io/normal/front/b/9/b90157f8-ba3c-479e-80d8-8f9e042c540c.jpg?1783928871",
    releaseDate = "2020-11-20",
    rarity = Rarity.COMMON,
)
