package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Genesis Wave reprint in Foundations. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Scars of Mirrodin `cards/` package
 * (earliest real printing); this file contributes only presentation data.
 */
val GenesisWaveReprint = Printing(
    oracleId = "e2487868-f386-438e-a73f-b494f6d35fac",
    name = "Genesis Wave",
    setCode = "FDN",
    collectorNumber = "221",
    scryfallId = "d46f7ddb-f986-4f1f-b096-ae1a02d0bdc8",
    artist = "Arif Wijaya",
    imageUri = "https://cards.scryfall.io/normal/front/d/4/d46f7ddb-f986-4f1f-b096-ae1a02d0bdc8.jpg?1783909059",
    releaseDate = "2024-11-15",
    rarity = Rarity.RARE,
)
