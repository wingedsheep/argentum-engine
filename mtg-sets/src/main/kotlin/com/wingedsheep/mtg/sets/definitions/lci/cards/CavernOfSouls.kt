package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Cavern of Souls reprint in LCI.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * AVR's `cards/` package (the card's earliest real printing). This file contributes
 * only the LCI-specific presentation row — set, collector number, art — picked up
 * automatically by `CardDiscovery.findPrintingsIn` and surfaced via the set's
 * `printings`.
 */
val CavernOfSoulsReprint = Printing(
    oracleId = "89ca686a-7c72-4d8f-9290-e89635624a83",
    name = "Cavern of Souls",
    setCode = "LCI",
    collectorNumber = "269",
    scryfallId = "3aad15a2-8a1b-4460-9b06-e85863081878",
    artist = "Alayna Danner",
    imageUri = "https://cards.scryfall.io/normal/front/3/a/3aad15a2-8a1b-4460-9b06-e85863081878.jpg?1706884128",
    releaseDate = "2023-11-17",
    rarity = Rarity.MYTHIC,
)
