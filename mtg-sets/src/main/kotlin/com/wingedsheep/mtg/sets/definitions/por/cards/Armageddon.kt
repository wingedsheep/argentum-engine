package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Armageddon reprint in POR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes
 * only the POR-specific presentation row — set, collector number, art — picked up
 * automatically by `CardDiscovery.findPrintingsIn` and surfaced via the set's
 * `printings`.
 */
val ArmageddonReprint = Printing(
    oracleId = "c9ed8b01-959a-47d6-891e-0abbdccf6e4f",
    name = "Armageddon",
    setCode = "POR",
    collectorNumber = "5",
    artist = "John Avon",
    imageUri = "https://cards.scryfall.io/normal/front/2/0/2073ca8b-2bca-4539-94d7-989da157e4b8.jpg",
    releaseDate = "1997-05-01",
    rarity = Rarity.RARE,
)
