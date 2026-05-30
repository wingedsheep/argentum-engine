package com.wingedsheep.mtg.sets.definitions.con.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Maniacal Rage reprint in Conflux. The canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in Exodus (`definitions/exo/cards/ManiacalRage.kt`); this file contributes only
 * presentation data for the Conflux printing.
 */
val ManiacalRageReprint = Printing(
    oracleId = "b5dbb212-0399-475b-a831-921f7c76cdc4",
    name = "Maniacal Rage",
    setCode = "CON",
    collectorNumber = "68",
    scryfallId = "abd98218-8318-40e7-8f36-68acce5d23a1",
    artist = "Brandon Kitkouski",
    imageUri = "https://cards.scryfall.io/normal/front/a/b/abd98218-8318-40e7-8f36-68acce5d23a1.jpg?1562803053",
    releaseDate = "2009-02-06",
    rarity = Rarity.COMMON,
)
