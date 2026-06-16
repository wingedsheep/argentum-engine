package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Noose Constrictor reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * EMN's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val NooseConstrictorReprint = Printing(
    oracleId = "fb1d521f-cb64-4b87-b3f4-a74e91a60349",
    name = "Noose Constrictor",
    setCode = "INR",
    collectorNumber = "210",
    artist = "Igor Kieryluk",
    imageUri = "https://cards.scryfall.io/normal/front/b/4/b432cccb-4291-46f3-a171-919adf8b95eb.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.UNCOMMON,
)
