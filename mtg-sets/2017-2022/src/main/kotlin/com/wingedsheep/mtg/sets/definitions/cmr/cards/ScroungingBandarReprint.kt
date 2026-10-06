package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Scrounging Bandar reprint in CMR. Canonical CardDefinition lives in Aether Revolt (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.aer.cards.ScroungingBandar`.
 */
val ScroungingBandarReprint = Printing(
    oracleId = "8e1eff5a-77e8-4ea2-96b8-547504f83b45",
    name = "Scrounging Bandar",
    setCode = "CMR",
    collectorNumber = "252",
    scryfallId = "02c8aab5-ed0d-489d-87af-7eb3193b75db",
    artist = "Shreya Shetty",
    imageUri = "https://cards.scryfall.io/normal/front/0/2/02c8aab5-ed0d-489d-87af-7eb3193b75db.jpg?1783928786",
    releaseDate = "2020-11-20",
    rarity = Rarity.COMMON,
)
