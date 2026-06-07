package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Cancel reprint in RTR. Canonical CardDefinition lives in its earliest set.
 */
val CancelReprint = Printing(
    oracleId = "7d00fb28-ea6c-49a9-b4af-ffb38860a9a7",
    name = "Cancel",
    setCode = "RTR",
    collectorNumber = "31",
    scryfallId = "fd994a26-65ff-43be-8d52-476e887d3ed2",
    artist = "Karl Kopinski",
    imageUri = "https://cards.scryfall.io/normal/front/f/d/fd994a26-65ff-43be-8d52-476e887d3ed2.jpg",
    releaseDate = "2012-10-05",
    rarity = Rarity.COMMON,
)
