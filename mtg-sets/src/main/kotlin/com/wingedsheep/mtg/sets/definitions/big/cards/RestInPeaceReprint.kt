package com.wingedsheep.mtg.sets.definitions.big.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Rest in Peace reprint in The Big Score. Canonical CardDefinition lives in
 * [com.wingedsheep.mtg.sets.definitions.rtr.cards.RestInPeace] (its earliest real printing,
 * Return to Ravnica); this file contributes only presentation data.
 */
val RestInPeaceReprint = Printing(
    oracleId = "087f9ad7-e74f-40e2-8102-1ed2925d0418",
    name = "Rest in Peace",
    setCode = "BIG",
    collectorNumber = "4",
    scryfallId = "d108c2b1-236e-4b8d-8445-d9749ccc4fea",
    artist = "Grady Frederick",
    imageUri = "https://cards.scryfall.io/normal/front/d/1/d108c2b1-236e-4b8d-8445-d9749ccc4fea.jpg?1739804156",
    releaseDate = "2024-04-19",
    rarity = Rarity.MYTHIC,
)
