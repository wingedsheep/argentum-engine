package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Coiling Oracle reprint in Commander Legends (CMR). Canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in Dissension (DIS). CMR printed two collector numbers: #443 and #688.
 */
val CoilingOracleReprint = Printing(
    oracleId = "69fd4ddf-9ed8-4c56-bef3-9944daf05e4f",
    name = "Coiling Oracle",
    setCode = "CMR",
    collectorNumber = "443",
    scryfallId = "e42b8b16-dd09-466b-ba1e-979e963ea384",
    artist = "Mark Zug",
    imageUri = "https://cards.scryfall.io/normal/front/e/4/e42b8b16-dd09-466b-ba1e-979e963ea384.jpg?1783928699",
    releaseDate = "2020-11-20",
    rarity = Rarity.COMMON,
)

val CoilingOracleReprintExtended = Printing(
    oracleId = "69fd4ddf-9ed8-4c56-bef3-9944daf05e4f",
    name = "Coiling Oracle",
    setCode = "CMR",
    collectorNumber = "688",
    scryfallId = "46ddfce6-5b8c-4ce1-a329-6a4a4576ca9c",
    artist = "Mark Zug",
    imageUri = "https://cards.scryfall.io/normal/front/4/6/46ddfce6-5b8c-4ce1-a329-6a4a4576ca9c.jpg?1783928603",
    releaseDate = "2020-11-20",
    rarity = Rarity.COMMON,
)
