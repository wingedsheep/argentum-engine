package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Ley Druid reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val LeyDruidReprint = Printing(
    oracleId = "a4ae41c7-9631-407d-8fd3-04f403d940a8",
    name = "Ley Druid",
    setCode = "LEB",
    collectorNumber = "206",
    artist = "Sandra Everingham",
    imageUri = "https://cards.scryfall.io/normal/front/b/5/b58867ec-0b1a-4804-bc2e-1c88d338c29e.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)
