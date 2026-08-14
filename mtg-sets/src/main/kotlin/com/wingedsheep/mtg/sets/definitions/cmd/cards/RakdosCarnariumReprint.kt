package com.wingedsheep.mtg.sets.definitions.cmd.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Rakdos Carnarium reprint in Commander 2011. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Dissension (`dis`) `cards/` package
 * (the card's earliest real printing); this file contributes only per-printing presentation data.
 */
val RakdosCarnariumReprint = Printing(
    oracleId = "0a023964-2905-4928-9c3e-dc63e6ebd218",
    name = "Rakdos Carnarium",
    setCode = "CMD",
    collectorNumber = "284",
    scryfallId = "560d2f1a-2872-43e4-86e2-5321117e4565",
    artist = "John Avon",
    imageUri = "https://cards.scryfall.io/normal/front/5/6/560d2f1a-2872-43e4-86e2-5321117e4565.jpg?1783941144",
    releaseDate = "2011-06-17",
    rarity = Rarity.COMMON,
)
