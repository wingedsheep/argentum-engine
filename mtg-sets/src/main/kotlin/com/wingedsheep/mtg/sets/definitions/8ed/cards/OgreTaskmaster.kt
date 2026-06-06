package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Ogre Taskmaster reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * P02's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val OgreTaskmasterReprint = Printing(
    oracleId = "5604f04e-fd72-41bf-906f-eb89d4a3475d",
    name = "Ogre Taskmaster",
    setCode = "8ED",
    collectorNumber = "205",
    artist = "Dany Orizio",
    imageUri = "https://cards.scryfall.io/normal/front/1/7/1765dee9-ab94-4ca5-9cd7-cf8e228fdd68.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)
