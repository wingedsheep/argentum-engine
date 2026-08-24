package com.wingedsheep.mtg.sets.definitions.pz2.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Pang Tong, "Young Phoenix" reprint in PZ2. Canonical CardDefinition lives in its earliest set.
 *
 * Hand-written rather than generated: `scripts/missing-reprints.py` scans for `card("Name")` with a
 * regex that stops at the first `"`, so a card name containing escaped quotes is invisible to it.
 */
val PangTongYoungPhoenixReprint = Printing(
    oracleId = "6d9dc632-414e-480c-85ef-c2b98a6460ad",
    name = "Pang Tong, \"Young Phoenix\"",
    setCode = "PZ2",
    collectorNumber = "65813",
    scryfallId = "dd0d76db-0521-440f-94e5-db1544a8c7ea",
    artist = "Li Tie",
    imageUri = "https://cards.scryfall.io/normal/front/d/d/dd0d76db-0521-440f-94e5-db1544a8c7ea.jpg",
    releaseDate = "2016-11-16",
    rarity = Rarity.RARE,
)
