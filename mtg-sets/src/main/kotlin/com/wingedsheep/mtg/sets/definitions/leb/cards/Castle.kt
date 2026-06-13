package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Castle reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val CastleReprint = Printing(
    oracleId = "f3179c3c-7e53-44d2-b579-b9e677efe9d9",
    name = "Castle",
    setCode = "LEB",
    collectorNumber = "9",
    artist = "Dameon Willich",
    imageUri = "https://cards.scryfall.io/normal/front/a/8/a8ba6b09-b24f-40cb-b219-ad8a1fd6692c.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)
