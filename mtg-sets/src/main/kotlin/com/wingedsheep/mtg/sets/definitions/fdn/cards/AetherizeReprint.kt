package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Aetherize reprint in FDN. Canonical [com.wingedsheep.sdk.model.CardDefinition] lives in
 * Gatecrash (GTC); this file contributes only the FDN presentation row.
 */
val AetherizeReprint = Printing(
    oracleId = "7c779721-cd1b-4696-9ae9-68ccc284ed2a",
    name = "Aetherize",
    setCode = "FDN",
    collectorNumber = "151",
    scryfallId = "1e5530fc-0291-4a17-b048-c5d24e6f51d8",
    artist = "Alexandre Honoré",
    imageUri = "https://cards.scryfall.io/normal/front/1/e/1e5530fc-0291-4a17-b048-c5d24e6f51d8.jpg?1782689136",
    releaseDate = "2024-11-15",
    rarity = Rarity.UNCOMMON,
)
