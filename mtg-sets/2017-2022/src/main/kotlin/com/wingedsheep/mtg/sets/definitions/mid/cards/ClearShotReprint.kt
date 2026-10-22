package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Clear Shot reprint in MID.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types) lives in
 * EMN's `cards/` package (the card's earliest real printing). This file contributes only
 * the MID-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ClearShotReprint = Printing(
    oracleId = "0b8defd2-8530-47d7-a0c6-27aa5162dff6",
    name = "Clear Shot",
    setCode = "MID",
    collectorNumber = "176",
    scryfallId = "27476d9c-006d-4227-a5f2-49c7234b84a1",
    artist = "Craig J Spearing",
    imageUri = "https://cards.scryfall.io/normal/front/2/7/27476d9c-006d-4227-a5f2-49c7234b84a1.jpg",
    releaseDate = "2021-09-24",
    rarity = Rarity.UNCOMMON,
)
