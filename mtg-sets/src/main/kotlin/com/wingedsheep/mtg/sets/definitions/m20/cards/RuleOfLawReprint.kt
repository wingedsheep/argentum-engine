package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Rule of Law reprint in M20. Canonical CardDefinition lives in Mirrodin (its earliest real
 * printing), `com.wingedsheep.mtg.sets.definitions.mrd.cards.RuleOfLaw`.
 */
val RuleOfLawReprint = Printing(
    oracleId = "53e88e64-6f82-4154-a66e-6aeb0154b368",
    name = "Rule of Law",
    setCode = "M20",
    collectorNumber = "35",
    scryfallId = "a1f4e79b-b103-4380-afa0-61a2b1773c9e",
    artist = "Scott M. Fischer",
    imageUri = "https://cards.scryfall.io/normal/front/a/1/a1f4e79b-b103-4380-afa0-61a2b1773c9e.jpg?1783933020",
    releaseDate = "2019-07-12",
    rarity = Rarity.UNCOMMON,
)
