package com.wingedsheep.mtg.sets.definitions.usg.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Bull Hippo reprint in USG.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * an earlier set's `cards/` package. This file contributes only the USG-specific
 * presentation row.
 */
val BullHippoReprint = Printing(
    oracleId = "4d62a448-b6a5-43b1-a281-9e9361a5524a",
    name = "Bull Hippo",
    setCode = "USG",
    collectorNumber = "239",
    scryfallId = "1d1f8259-1825-4a46-8026-75adc4480322",
    artist = "Daren Bader",
    imageUri = "https://cards.scryfall.io/normal/front/1/d/1d1f8259-1825-4a46-8026-75adc4480322.jpg?1562901144",
    releaseDate = "1998-10-12",
    rarity = Rarity.UNCOMMON,
)
