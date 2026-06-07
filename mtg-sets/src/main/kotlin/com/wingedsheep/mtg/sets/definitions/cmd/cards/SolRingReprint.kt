package com.wingedsheep.mtg.sets.definitions.cmd.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Sol Ring reprint in CMD. Canonical CardDefinition lives in its earliest set.
 */
val SolRingReprint = Printing(
    oracleId = "6ad8011d-3471-4369-9d68-b264cc027487",
    name = "Sol Ring",
    setCode = "CMD",
    collectorNumber = "261",
    scryfallId = "71357a3d-9a9f-4ec6-8e01-1966b220206c",
    artist = "Mike Bierek",
    imageUri = "https://cards.scryfall.io/normal/front/7/1/71357a3d-9a9f-4ec6-8e01-1966b220206c.jpg",
    releaseDate = "2011-06-17",
    rarity = Rarity.UNCOMMON,
)
