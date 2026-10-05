package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Unquestioned Authority reprint in CMR. Canonical CardDefinition lives in Judgment (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.jud.cards.UnquestionedAuthority`.
 */
val UnquestionedAuthorityReprint = Printing(
    oracleId = "3d50266f-5a2f-4a48-ab15-87e733624fd4",
    name = "Unquestioned Authority",
    setCode = "CMR",
    collectorNumber = "389",
    scryfallId = "829710f9-6554-4391-bdb1-2ccdefc2caf5",
    artist = "Zoltan Boros",
    imageUri = "https://cards.scryfall.io/normal/front/8/2/829710f9-6554-4391-bdb1-2ccdefc2caf5.jpg?1783928723",
    releaseDate = "2020-11-20",
    rarity = Rarity.UNCOMMON,
)
