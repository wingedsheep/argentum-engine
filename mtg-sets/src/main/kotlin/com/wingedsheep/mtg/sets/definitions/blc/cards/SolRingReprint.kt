package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Sol Ring reprint in Bloomburrow Commander. Canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in `definitions/lea/cards/SolRing.kt`; this file contributes only presentation data.
 */
val SolRingReprint = Printing(
    oracleId = "6ad8011d-3471-4369-9d68-b264cc027487",
    name = "Sol Ring",
    setCode = "BLC",
    collectorNumber = "129",
    scryfallId = "fff440c0-7e6a-46c2-9989-f1b5af20fd44",
    artist = "Mike Dringenberg",
    imageUri = "https://cards.scryfall.io/normal/front/f/f/fff440c0-7e6a-46c2-9989-f1b5af20fd44.jpg?1721428809",
    releaseDate = "2024-08-02",
    rarity = Rarity.UNCOMMON,
)
