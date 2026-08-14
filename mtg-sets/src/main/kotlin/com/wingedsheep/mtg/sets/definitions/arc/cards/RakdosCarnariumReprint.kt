package com.wingedsheep.mtg.sets.definitions.arc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Rakdos Carnarium reprint in Archenemy. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Dissension (`dis`) `cards/` package
 * (the card's earliest real printing); this file contributes only per-printing presentation data.
 */
val RakdosCarnariumReprint = Printing(
    oracleId = "0a023964-2905-4928-9c3e-dc63e6ebd218",
    name = "Rakdos Carnarium",
    setCode = "ARC",
    collectorNumber = "132",
    scryfallId = "ce676c84-375f-4407-aaf3-a2f0a9b741a9",
    artist = "John Avon",
    imageUri = "https://cards.scryfall.io/normal/front/c/e/ce676c84-375f-4407-aaf3-a2f0a9b741a9.jpg?1783941885",
    releaseDate = "2010-06-18",
    rarity = Rarity.COMMON,
)
