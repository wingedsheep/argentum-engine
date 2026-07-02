package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Crusader of Odric reprint in FDN. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in Magic 2013's `cards/` package;
 * this file contributes only the FDN presentation row.
 */
val CrusaderOfOdricReprint = Printing(
    oracleId = "ea384b0d-3091-4d50-b15f-1f6763647b7c",
    name = "Crusader of Odric",
    setCode = "FDN",
    collectorNumber = "731",
    scryfallId = "2fd0400a-ef3e-4b03-9852-63e28304da53",
    artist = "Michael Komarck",
    imageUri = "https://cards.scryfall.io/normal/front/2/f/2fd0400a-ef3e-4b03-9852-63e28304da53.jpg?1782683484",
    releaseDate = "2026-04-24",
    rarity = Rarity.COMMON,
)
