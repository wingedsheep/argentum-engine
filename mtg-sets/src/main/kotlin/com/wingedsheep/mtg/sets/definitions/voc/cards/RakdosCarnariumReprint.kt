package com.wingedsheep.mtg.sets.definitions.voc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Rakdos Carnarium reprint in Innistrad: Crimson Vow Commander. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Dissension (`dis`) `cards/` package
 * (the card's earliest real printing); this file contributes only per-printing presentation data.
 */
val RakdosCarnariumReprint = Printing(
    oracleId = "0a023964-2905-4928-9c3e-dc63e6ebd218",
    name = "Rakdos Carnarium",
    setCode = "VOC",
    collectorNumber = "180",
    scryfallId = "385e7ce2-0484-4763-949a-10082cd60e46",
    artist = "John Avon",
    imageUri = "https://cards.scryfall.io/normal/front/3/8/385e7ce2-0484-4763-949a-10082cd60e46.jpg?1783924933",
    releaseDate = "2021-11-19",
    rarity = Rarity.COMMON,
)
