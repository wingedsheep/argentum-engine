package com.wingedsheep.mtg.sets.definitions.s99.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Remove Soul reprint in S99. Canonical CardDefinition lives in its earliest set.
 */
val RemoveSoulReprint = Printing(
    oracleId = "b13c0f76-fbda-4911-9442-c3d7e97f1aac",
    name = "Remove Soul",
    setCode = "S99",
    collectorNumber = "49",
    scryfallId = "0415833b-2185-4de5-b184-d185e76835ae",
    artist = "Mike Dringenberg",
    imageUri = "https://cards.scryfall.io/normal/front/0/4/0415833b-2185-4de5-b184-d185e76835ae.jpg",
    releaseDate = "1999-07-01",
    rarity = Rarity.COMMON,
)
