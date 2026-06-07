package com.wingedsheep.mtg.sets.definitions.cmd.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Terminate reprint in CMD. Canonical CardDefinition lives in its earliest set.
 */
val TerminateReprint = Printing(
    oracleId = "6257c2fd-005f-41e3-8a72-af76df1eb134",
    name = "Terminate",
    setCode = "CMD",
    collectorNumber = "231",
    scryfallId = "fc7345ac-02f8-4cc3-9d11-73ec00d63913",
    artist = "Wayne Reynolds",
    imageUri = "https://cards.scryfall.io/normal/front/f/c/fc7345ac-02f8-4cc3-9d11-73ec00d63913.jpg",
    releaseDate = "2011-06-17",
    rarity = Rarity.COMMON,
)
