package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Valorous Stance reprint in VOW.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types) lives in FRF's
 * `cards/` package (the card's earliest real printing). This file contributes only the
 * VOW-specific presentation row — set, collector number, art — picked up automatically by
 * `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ValorousStanceReprint = Printing(
    oracleId = "1f938353-9081-4dc1-b8e7-d18ece038bd4",
    name = "Valorous Stance",
    setCode = "VOW",
    collectorNumber = "42",
    scryfallId = "0e6b9a3b-8a19-4094-8dbb-08a0a9ca04a0",
    artist = "Anato Finnstark",
    imageUri = "https://cards.scryfall.io/normal/front/0/e/0e6b9a3b-8a19-4094-8dbb-08a0a9ca04a0.jpg?1782703164",
    releaseDate = "2021-11-19",
    rarity = Rarity.UNCOMMON,
)
