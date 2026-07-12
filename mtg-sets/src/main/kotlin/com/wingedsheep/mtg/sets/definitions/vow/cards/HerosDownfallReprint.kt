package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Hero's Downfall reprint in VOW.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types) lives in THS's
 * `cards/` package (the card's earliest real printing). This file contributes only the
 * VOW-specific presentation row — set, collector number, art — picked up automatically by
 * `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val HerosDownfallReprint = Printing(
    oracleId = "03df6a57-37c9-46d3-83b3-4a6240100714",
    name = "Hero's Downfall",
    setCode = "VOW",
    collectorNumber = "120",
    scryfallId = "c1b0751e-3a7e-4568-8c64-7429d6829687",
    artist = "Chris Rallis",
    imageUri = "https://cards.scryfall.io/normal/front/c/1/c1b0751e-3a7e-4568-8c64-7429d6829687.jpg?1782703105",
    releaseDate = "2021-11-19",
    rarity = Rarity.UNCOMMON,
)
