package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Suntail Hawk reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * JUD's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SuntailHawkReprint = Printing(
    oracleId = "a8d31e2f-7b2e-4135-8074-9e6ef778bd80",
    name = "Suntail Hawk",
    setCode = "10E",
    collectorNumber = "50",
    artist = "Heather Hudson",
    imageUri = "https://cards.scryfall.io/normal/front/b/4/b4886566-af41-4d14-8ae1-ce2952db8e42.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
