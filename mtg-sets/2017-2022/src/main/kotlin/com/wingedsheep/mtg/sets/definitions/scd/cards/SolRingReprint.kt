package com.wingedsheep.mtg.sets.definitions.scd.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Sol Ring reprint in SCD. Canonical CardDefinition lives in its earliest set.
 */
val SolRingReprint = Printing(
    oracleId = "6ad8011d-3471-4369-9d68-b264cc027487",
    name = "Sol Ring",
    setCode = "SCD",
    collectorNumber = "276",
    scryfallId = "1b023575-4cb5-434f-92ef-d3b81aaa3c3e",
    artist = "Mike Bierek",
    imageUri = "https://cards.scryfall.io/normal/front/1/b/1b023575-4cb5-434f-92ef-d3b81aaa3c3e.jpg",
    releaseDate = "2022-12-02",
    rarity = Rarity.UNCOMMON,
)
