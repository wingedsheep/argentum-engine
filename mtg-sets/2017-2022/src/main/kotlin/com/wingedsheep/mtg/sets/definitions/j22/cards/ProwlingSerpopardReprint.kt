package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Prowling Serpopard reprint in J22. Canonical CardDefinition lives in Amonkhet (its earliest
 * real printing), `com.wingedsheep.mtg.sets.definitions.akh.cards.ProwlingSerpopard`.
 */
val ProwlingSerpopardReprint = Printing(
    oracleId = "4bdfd718-f474-4223-88d8-7fa9fb0c86b4",
    name = "Prowling Serpopard",
    setCode = "J22",
    collectorNumber = "713",
    scryfallId = "88d1c576-0203-4ede-8728-1efa87c33d20",
    artist = "Tyler Jacobson",
    imageUri = "https://cards.scryfall.io/normal/front/8/8/88d1c576-0203-4ede-8728-1efa87c33d20.jpg?1783918842",
    releaseDate = "2022-12-02",
    rarity = Rarity.RARE,
)
