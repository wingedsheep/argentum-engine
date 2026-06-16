package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Flashfreeze reprint in FDN.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * CSP's `cards/` package (the card's earliest real printing). This file contributes only
 * the FDN-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val FlashfreezeReprint = Printing(
    oracleId = "eaf98e03-729b-4145-b2af-c910c415c15d",
    name = "Flashfreeze",
    setCode = "FDN",
    collectorNumber = "590",
    artist = "Brian Despain",
    imageUri = "https://cards.scryfall.io/normal/front/9/1/91e37a7e-6093-4ef5-b6e4-4aa800ddfc1b.jpg",
    releaseDate = "2024-11-15",
    rarity = Rarity.UNCOMMON,
)
