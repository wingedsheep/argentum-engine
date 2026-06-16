package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Wall of Bone reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val WallOfBoneReprint = Printing(
    oracleId = "8ffe4986-9e09-4421-ad26-296a4c0df9e4",
    name = "Wall of Bone",
    setCode = "LEB",
    collectorNumber = "133",
    artist = "Anson Maddocks",
    imageUri = "https://cards.scryfall.io/normal/front/7/9/7930666c-12ac-420b-8ced-0e924925b075.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)
