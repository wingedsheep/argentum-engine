package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Boros Garrison reprint in Commander Legends. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Ravnica: City of Guilds (`rav`)
 * `cards/` package (the card's earliest real printing); this file contributes only
 * per-printing presentation data.
 */
val BorosGarrisonReprint = Printing(
    oracleId = "8fa3ac81-3dfe-4565-be99-5554f7597b4b",
    name = "Boros Garrison",
    setCode = "CMR",
    collectorNumber = "477",
    scryfallId = "7bba2ac2-15dc-4c43-bb57-9943444ce1d7",
    artist = "John Avon",
    imageUri = "https://cards.scryfall.io/normal/front/7/b/7bba2ac2-15dc-4c43-bb57-9943444ce1d7.jpg?1783928686",
    releaseDate = "2020-11-20",
    rarity = Rarity.UNCOMMON,
)
