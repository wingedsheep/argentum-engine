package com.wingedsheep.mtg.sets.definitions.jmp.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Ghoulraiser reprint in JMP. Canonical CardDefinition lives in Innistrad (its earliest real
 * printing), `com.wingedsheep.mtg.sets.definitions.isd.cards.Ghoulraiser`.
 */
val GhoulraiserJmpReprint = Printing(
    oracleId = "614f748b-5d31-440d-a7c0-3ba12dc63c24",
    name = "Ghoulraiser",
    setCode = "JMP",
    collectorNumber = "238",
    scryfallId = "850ccdcb-2cd7-4f27-aa9b-917a62a5e94d",
    artist = "Steve Prescott",
    imageUri = "https://cards.scryfall.io/normal/front/8/5/850ccdcb-2cd7-4f27-aa9b-917a62a5e94d.jpg?1783930423",
    releaseDate = "2020-07-17",
    rarity = Rarity.COMMON,
)
