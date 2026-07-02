package com.wingedsheep.mtg.sets.definitions.jmp.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Chart a Course reprint in Jumpstart (JMP). The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in Ixalan's `cards/` package;
 * this file contributes only the JMP presentation row.
 */
val ChartACourseReprint = Printing(
    oracleId = "05878e49-93ad-4144-9c50-a0bb86126c2e",
    name = "Chart a Course",
    setCode = "JMP",
    collectorNumber = "142",
    scryfallId = "1a6f256b-943e-4cfb-9fc9-d1ded68b5f97",
    artist = "James Ryman",
    imageUri = "https://cards.scryfall.io/normal/front/1/a/1a6f256b-943e-4cfb-9fc9-d1ded68b5f97.jpg?1782706688",
    releaseDate = "2020-07-17",
    rarity = Rarity.UNCOMMON,
)
