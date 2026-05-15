package com.wingedsheep.mtg.sets.definitions.usg.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Anaconda reprint in USG.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * an earlier set's `cards/` package. This file contributes only the USG-specific
 * presentation row.
 */
val AnacondaReprint = Printing(
    oracleId = "3eff03f1-2c5f-4c59-b465-a8c4cd05e1ba",
    name = "Anaconda",
    setCode = "USG",
    collectorNumber = "232",
    scryfallId = "1be798fd-18c9-45b0-8207-7e5e01c83f49",
    artist = "Stephen Daniele",
    imageUri = "https://cards.scryfall.io/normal/front/1/b/1be798fd-18c9-45b0-8207-7e5e01c83f49.jpg?1562900839",
    releaseDate = "1998-10-12",
    rarity = Rarity.UNCOMMON,
)
