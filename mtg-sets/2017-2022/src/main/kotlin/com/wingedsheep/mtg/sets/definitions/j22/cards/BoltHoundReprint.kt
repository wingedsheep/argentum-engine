package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Bolt Hound reprint in J22. Canonical CardDefinition lives in Core Set 2021 (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.m21.cards.BoltHound`.
 */
val BoltHoundReprint = Printing(
    oracleId = "60555f06-dc85-4a34-ae3c-fc1cdb923be7",
    name = "Bolt Hound",
    setCode = "J22",
    collectorNumber = "504",
    scryfallId = "3df5b536-6098-4352-8172-ec11d656d4a9",
    artist = "Forrest Imel",
    imageUri = "https://cards.scryfall.io/normal/front/3/d/3df5b536-6098-4352-8172-ec11d656d4a9.jpg?1783918960",
    releaseDate = "2022-12-02",
    rarity = Rarity.UNCOMMON,
)
