package com.wingedsheep.mtg.sets.definitions.tmp.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Charging Rhino reprint in TMP.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * an earlier set's `cards/` package. This file contributes only the TMP-specific
 * presentation row.
 */
val ChargingRhinoReprint = Printing(
    oracleId = "26966ecb-15d3-47e5-ab63-e38510c87ecc",
    name = "Charging Rhino",
    setCode = "TMP",
    collectorNumber = "218",
    scryfallId = "651f89e5-9ce2-4713-aca9-6581005f6ca2",
    artist = "Daren Bader",
    imageUri = "https://cards.scryfall.io/normal/front/6/5/651f89e5-9ce2-4713-aca9-6581005f6ca2.jpg?1562054257",
    releaseDate = "1997-10-14",
    rarity = Rarity.UNCOMMON,
)
