package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Annul reprint in EOE.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * USG's `cards/` package (the card's earliest real printing). This file contributes
 * only the EOE-specific presentation row — set, collector number, art — picked up
 * automatically by `CardDiscovery.findPrintingsIn` and surfaced via the set's
 * `printings`.
 */
val AnnulReprint = Printing(
    oracleId = "d08e9784-75f7-4164-ac48-d06160f8c56b",
    name = "Annul",
    setCode = "EOE",
    collectorNumber = "46",
    scryfallId = "4feeebea-aa55-4599-ab5a-4e41a54d0dfd",
    artist = "Carlos Palma Cruchaga",
    imageUri = "https://cards.scryfall.io/normal/front/4/f/4feeebea-aa55-4599-ab5a-4e41a54d0dfd.jpg?1752946732",
    releaseDate = "2025-08-01",
    rarity = Rarity.UNCOMMON,
)
