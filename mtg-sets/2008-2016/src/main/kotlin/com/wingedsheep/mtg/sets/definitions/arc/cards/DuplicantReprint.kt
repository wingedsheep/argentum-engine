package com.wingedsheep.mtg.sets.definitions.arc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Duplicant reprint in Archenemy. Canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in Mirrodin (`mrd`) — earliest real printing; this file contributes only
 * presentation data.
 */
val DuplicantReprint = Printing(
    oracleId = "ea86abfa-6cab-4ef0-8463-34136fc25b59",
    name = "Duplicant",
    setCode = "ARC",
    collectorNumber = "106",
    scryfallId = "653fa830-5cd0-4798-94be-e7644f462c26",
    artist = "Thomas M. Baxa",
    imageUri = "https://cards.scryfall.io/normal/front/6/5/653fa830-5cd0-4798-94be-e7644f462c26.jpg?1783941892",
    releaseDate = "2010-06-18",
    rarity = Rarity.RARE,
)
